package com.jr.englishword.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.IOException

/** 词库与设置仓库：内存 StateFlow + JSON 文件持久化。 */
class Repository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val wordsFile = File(appContext.filesDir, "words.json")
    private val settingsFile = File(appContext.filesDir, "api_settings.json")
    private val wrongFile = File(appContext.filesDir, "wrongbook.json")

    private val _words = MutableStateFlow<List<WordEntry>>(emptyList())
    val words: StateFlow<List<WordEntry>> = _words

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings

    private val _wrong = MutableStateFlow<List<WrongRecord>>(emptyList())
    val wrong: StateFlow<List<WrongRecord>> = _wrong

    /** 读取失败（文件损坏）提示；null 表示正常。 */
    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError

    /** 落盘失败提示；null 表示正常。 */
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError

    /**
     * 每个文件一个「待写入」信号。CONFLATED 只保留一个未处理信号，且写入时才读取当前状态，
     * 因此连续修改既不会造成写放大，也不会出现旧快照覆盖新快照。
     * 每个文件只有这一个消费者协程，同一文件不会并发写入。
     */
    private val wordsSignal = Channel<Unit>(Channel.CONFLATED)
    private val settingsSignal = Channel<Unit>(Channel.CONFLATED)
    private val wrongSignal = Channel<Unit>(Channel.CONFLATED)

    init {
        if (wordsFile.exists()) {
            runCatching { json.decodeFromString<List<WordEntry>>(wordsFile.readText()) }
                .onSuccess { _words.value = it }
                .onFailure { onLoadFailure(wordsFile, it) }
        }
        if (settingsFile.exists()) {
            runCatching {
                val raw = settingsFile.readText()
                if (raw.isBlank()) AppSettings() else json.decodeFromString<AppSettings>(raw)
            }
                .onSuccess { _settings.value = it }
                .onFailure { onLoadFailure(settingsFile, it) }
        }
        if (wrongFile.exists()) {
            runCatching { json.decodeFromString<List<WrongRecord>>(wrongFile.readText()) }
                .onSuccess { _wrong.value = it }
                .onFailure { onLoadFailure(wrongFile, it) }
        }
        scope.launch { while (true) { wordsSignal.receive(); writeWords() } }
        scope.launch { while (true) { settingsSignal.receive(); writeSettings() } }
        scope.launch { while (true) { wrongSignal.receive(); writeWrong() } }
    }

    /** 导入单词；replace=false 时按（单词+词性）去重追加。返回实际新增数量。 */
    fun addWords(list: List<WordEntry>, replace: Boolean): Int {
        if (list.isEmpty()) return 0
        if (replace) {
            _words.value = list
            // 词条被整体替换后，旧 ID 的错题记录已无对应单词，一并清理
            _wrong.value = emptyList()
            persistWords()
            persistWrong()
            return list.size
        }
        val existing = _words.value.mapTo(HashSet()) { it.word.lowercase() + "|" + it.pos.lowercase() }
        val fresh = list.filter { existing.add(it.word.lowercase() + "|" + it.pos.lowercase()) }
        if (fresh.isNotEmpty()) {
            _words.value = _words.value + fresh
            persistWords()
        }
        return fresh.size
    }

    fun updateWords(transform: (List<WordEntry>) -> List<WordEntry>) {
        _words.value = transform(_words.value)
        persistWords()
    }

    fun recordResult(id: String, isCorrect: Boolean) {
        _words.value = _words.value.map {
            if (it.id == id) {
                if (isCorrect) it.copy(correct = it.correct + 1)
                else it.copy(wrong = it.wrong + 1)
            } else it
        }
        persistWords()
    }

    /** 加入/累计错题记录；答对时可调用 removeWrong 移出。 */
    fun addWrong(wordId: String) {
        val now = System.currentTimeMillis()
        val list = _wrong.value
        val idx = list.indexOfFirst { it.wordId == wordId }
        _wrong.value = if (idx >= 0) {
            list.toMutableList().also {
                it[idx] = list[idx].copy(count = list[idx].count + 1, lastWrongAt = now)
            }
        } else {
            list + WrongRecord(wordId, 1, now)
        }
        persistWrong()
    }

    fun removeWrong(wordId: String) {
        _wrong.value = _wrong.value.filterNot { it.wordId == wordId }
        persistWrong()
    }

    /** 读取某单词当前的错题记录，供删除/移出后撤销时还原原值。 */
    fun wrongRecordOf(wordId: String): WrongRecord? =
        _wrong.value.firstOrNull { it.wordId == wordId }

    /** 按原值还原错题记录；与 addWrong 不同，不会重置错误次数与时间。 */
    fun restoreWrong(record: WrongRecord) {
        if (_wrong.value.any { it.wordId == record.wordId }) return
        _wrong.value = _wrong.value + record
        persistWrong()
    }

    fun clearWrong() {
        _wrong.value = emptyList()
        persistWrong()
    }

    /** 保存 API 配置并触发落盘 */
    fun updateSettings(s: AppSettings) {
        _settings.value = s
        persistSettings()
    }

    fun clearLoadError() {
        _loadError.value = null
    }

    fun clearSaveError() {
        _saveError.value = null
    }

    /** 读取失败时备份原文件并给出提示，避免静默降级为空数据。 */
    private fun onLoadFailure(file: File, e: Throwable) {
        val backup = File(file.parentFile, file.name + ".corrupt-" + System.currentTimeMillis())
        runCatching { file.copyTo(backup, overwrite = true) }
        _loadError.value = "${file.name} 无法读取，本次按空数据启动；原文件已备份为 ${backup.name}。" +
            "原因：${e.message ?: e.javaClass.simpleName}"
    }

    private fun reportSaveFailure(what: String, e: Throwable) {
        _saveError.value = "${what}保存失败：${e.message ?: e.javaClass.simpleName}"
    }

    private fun persistWords() {
        wordsSignal.trySend(Unit)
    }

    private fun persistWrong() {
        wrongSignal.trySend(Unit)
    }

    private fun persistSettings() {
        settingsSignal.trySend(Unit)
    }

    private fun writeWords() {
        val snapshot = _words.value
        runCatching { writeAtomic(wordsFile, json.encodeToString(snapshot)) }
            .onFailure { reportSaveFailure("词库", it) }
    }

    private fun writeSettings() {
        val snapshot = _settings.value
        runCatching { writeAtomic(settingsFile, json.encodeToString(snapshot)) }
            .onFailure { reportSaveFailure("设置", it) }
    }

    private fun writeWrong() {
        val snapshot = _wrong.value
        runCatching { writeAtomic(wrongFile, json.encodeToString(snapshot)) }
            .onFailure { reportSaveFailure("错题本", it) }
    }

    private fun writeAtomic(target: File, content: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(content)
        // 同目录 rename 在 Android（Linux）上是原子替换，避免「先删后改名」之间的空窗
        if (!tmp.renameTo(target)) {
            if (target.exists() && !target.delete()) throw IOException("无法删除 ${target.name}")
            if (!tmp.renameTo(target)) throw IOException("无法写入 ${target.name}")
        }
    }

    companion object {
        @Volatile
        private var instance: Repository? = null

        fun get(context: Context): Repository =
            instance ?: synchronized(this) {
                instance ?: Repository(context).also { instance = it }
            }
    }
}
