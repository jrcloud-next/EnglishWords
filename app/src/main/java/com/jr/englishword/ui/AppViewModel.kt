package com.jr.englishword.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.jr.englishword.data.AppSettings
import com.jr.englishword.data.Repository
import com.jr.englishword.data.WordEntry
import com.jr.englishword.data.WrongRecord
import com.jr.englishword.net.AiUsageSnapshot
import com.jr.englishword.net.Api
import kotlinx.coroutines.flow.StateFlow

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository.get(app)

    val words: StateFlow<List<WordEntry>> = repo.words
    val settings: StateFlow<AppSettings> = repo.settings
    val wrong: StateFlow<List<WrongRecord>> = repo.wrong

    /** 最近一次成功 AI 业务请求的用量，仅在当前应用进程内保留。 */
    val lastBusinessUsage: StateFlow<AiUsageSnapshot?> = Api.lastBusinessUsage

    /** 词库/设置/错题本读取失败提示；null 表示正常。 */
    val loadError: StateFlow<String?> = repo.loadError

    /** 落盘失败提示；null 表示正常。 */
    val saveError: StateFlow<String?> = repo.saveError

    fun addWords(list: List<WordEntry>, replace: Boolean): Int = repo.addWords(list, replace)

    fun updateWords(transform: (List<WordEntry>) -> List<WordEntry>) = repo.updateWords(transform)

    fun updateSettings(s: AppSettings) = repo.updateSettings(s)

    fun recordResult(id: String, isCorrect: Boolean) = repo.recordResult(id, isCorrect)

    fun addWrong(wordId: String) = repo.addWrong(wordId)

    fun removeWrong(wordId: String) = repo.removeWrong(wordId)

    /** 读取当前错题记录，供撤销时还原原次数与时间。 */
    fun wrongRecordOf(wordId: String): WrongRecord? = repo.wrongRecordOf(wordId)

    /** 按原值还原错题记录。 */
    fun restoreWrong(record: WrongRecord) = repo.restoreWrong(record)

    fun clearWrong() = repo.clearWrong()

    fun clearLoadError() = repo.clearLoadError()

    fun clearSaveError() = repo.clearSaveError()

    suspend fun aiDistractors(word: WordEntry, chinese: Boolean): List<String> =
        Api.aiDistractors(
            settings.value,
            word.word,
            word.pos,
            word.meaning,
            chinese
        )

    suspend fun moreInfo(word: WordEntry): Api.AiInfoResult =
        Api.moreInfo(settings.value, word.word, word.pos)
}
