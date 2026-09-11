package com.jr.englishword.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.jr.englishword.data.AppSettings
import com.jr.englishword.data.Repository
import com.jr.englishword.data.WordEntry
import com.jr.englishword.data.WrongRecord
import com.jr.englishword.net.DeepSeekApi
import kotlinx.coroutines.flow.StateFlow

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository.get(app)

    val words: StateFlow<List<WordEntry>> = repo.words
    val settings: StateFlow<AppSettings> = repo.settings
    val wrong: StateFlow<List<WrongRecord>> = repo.wrong

    fun addWords(list: List<WordEntry>, replace: Boolean): Int = repo.addWords(list, replace)

    fun updateWords(transform: (List<WordEntry>) -> List<WordEntry>) = repo.updateWords(transform)

    fun updateSettings(s: AppSettings) = repo.updateSettings(s)

    fun recordResult(id: String, isCorrect: Boolean) = repo.recordResult(id, isCorrect)

    fun addWrong(wordId: String) = repo.addWrong(wordId)

    fun removeWrong(wordId: String) = repo.removeWrong(wordId)

    fun clearWrong() = repo.clearWrong()

    suspend fun aiDistractors(word: WordEntry, chinese: Boolean): List<String> {
        val s = settings.value
        if (!s.apiEnabled) return emptyList()
        return DeepSeekApi.aiDistractors(
            s,
            word.word,
            word.pos,
            if (chinese) word.meaning else word.word,
            chinese
        )
    }

    suspend fun moreInfo(word: WordEntry): DeepSeekApi.AiInfoResult =
        DeepSeekApi.moreInfo(settings.value, word.word, word.pos)
}
