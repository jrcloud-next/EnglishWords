package com.jr.englishword.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** 词库与设置仓库：内存 StateFlow + JSON 文件持久化。 */
class Repository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }
    private val wordsMutex = Mutex()
    private val settingsMutex = Mutex()
    private val wrongMutex = Mutex()

    private val wordsFile = File(appContext.filesDir, "words.json")
    private val settingsFile = File(appContext.filesDir, "api_settings.json")
    private val wrongFile = File(appContext.filesDir, "wrongbook.json")

    private val _words = MutableStateFlow<List<WordEntry>>(emptyList())
    val words: StateFlow<List<WordEntry>> = _words

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings

    private val _wrong = MutableStateFlow<List<WrongRecord>>(emptyList())
    val wrong: StateFlow<List<WrongRecord>> = _wrong

    init {
        runCatching {
            if (wordsFile.exists()) {
                _words.value = json.decodeFromString<List<WordEntry>>(wordsFile.readText())
            }
        }
        runCatching {
            if (settingsFile.exists()) {
                val raw = settingsFile.readText()
                if (raw.isNotBlank()) {
                    _settings.value = json.decodeFromString<AppSettings>(raw)
                }
            }
        }
        runCatching {
            if (wrongFile.exists()) {
                _wrong.value = json.decodeFromString<List<WrongRecord>>(wrongFile.readText())
            }
        }
    }

    /** 导入单词；replace=false 时按（单词+词性）去重追加。返回实际新增数量。 */
    fun addWords(list: List<WordEntry>, replace: Boolean): Int {
        if (list.isEmpty()) return 0
        if (replace) {
            _words.value = list
            persistWords()
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

    fun clearWrong() {
        _wrong.value = emptyList()
        persistWrong()
    }

    private fun persistWrong() {
        val snapshot = _wrong.value
        scope.launch {
            wrongMutex.withLock {
                runCatching { writeAtomic(wrongFile, json.encodeToString(snapshot)) }
            }
        }
    }

    /** 保存 API 配置到磁盘并更新内存状态 */
    fun updateSettings(s: AppSettings) {
        _settings.value = s
        scope.launch {
            settingsMutex.withLock {
                runCatching { writeAtomic(settingsFile, json.encodeToString(s)) }
            }
        }
    }

    private fun persistWords() {
        val snapshot = _words.value
        scope.launch {
            wordsMutex.withLock {
                runCatching { writeAtomic(wordsFile, json.encodeToString(snapshot)) }
            }
        }
    }

    private fun writeAtomic(target: File, content: String) {
        val tmp = File(target.parentFile, target.name + ".tmp")
        tmp.writeText(content)
        if (target.exists()) target.delete()
        tmp.renameTo(target)
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
