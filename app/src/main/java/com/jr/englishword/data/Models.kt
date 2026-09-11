package com.jr.englishword.data

import kotlinx.serialization.Serializable

@Serializable
data class WordEntry(
    val id: String,
    val word: String,
    val pos: String = "",
    val meaning: String,
    val correct: Int = 0,
    val wrong: Int = 0
) {
    val practiced: Int get() = correct + wrong
    val mastery: Float get() = if (practiced == 0) 0f else correct.toFloat() / practiced
}

@Serializable
data class WrongRecord(
    val wordId: String,
    val count: Int = 1,
    val lastWrongAt: Long = 0L
)

@Serializable
data class AppSettings(
    val apiEnabled: Boolean = true,
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
    val questionsPerRound: Int = 10,
    val aiDistractors: Boolean = true
)

enum class QuizMode(val title: String, val shortDesc: String) {
    EN_CN("英译中", "看英文 · 四选一选中文释义"),
    CN_EN("中译英", "看中文 · 四选一选英文单词"),
    DICTATION("默写中文", "看英文 · 默写中文意思"),
    SPELLING("拼写英文", "看中文 · 默写英文单词")
}
