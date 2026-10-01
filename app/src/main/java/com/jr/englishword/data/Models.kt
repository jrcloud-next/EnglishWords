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
    val questionsPerRound: Int = 10
)

enum class QuizMode(val title: String) {
    EN_CN("英译中"),
    CN_EN("中译英"),
    DICTATION("默写中文"),
    SPELLING("拼写英文")
}
