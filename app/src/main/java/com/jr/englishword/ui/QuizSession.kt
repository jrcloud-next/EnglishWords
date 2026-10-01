package com.jr.englishword.ui

import androidx.compose.runtime.saveable.Saver
import com.jr.englishword.data.WordEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 一道题。选项与正确答案下标在干扰项就绪后一次性写入，此后不再改动。 */
@Serializable
data class Question(
    val word: WordEntry,
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1
)

/** 每道题的作答状态，支持在题目间前进/后退时还原。 */
@Serializable
data class AnswerState(
    val selected: Int = -1,
    val typed: String = "",
    val answered: Boolean = false,
    val correct: Boolean = false
)

/**
 * 一整轮练习的完整状态，整体作为一个 rememberSaveable 值保存。
 * 这样屏幕旋转或进程重建后既不会丢失本轮进度，也不会把「错题重练」变成全词库练习。
 */
@Serializable
data class QuizSession(
    /** 生成这批题目时的种子（模式|轮次|题源）。seed 相同即表示题目已就绪，无需重新生成。 */
    val seed: String = "",
    val restartKey: Int = 0,
    val questions: List<Question> = emptyList(),
    val answers: List<AnswerState> = emptyList(),
    val index: Int = 0,
    val score: Int = 0,
    val wrongWords: List<WordEntry> = emptyList(),
    val preparing: Boolean = true
)

private val saveableJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** 会话 ↔ JSON 字符串。String 是 Bundle 原生支持的类型，因此可交给 rememberSaveable。 */
val QuizSessionSaver: Saver<QuizSession, String> = Saver(
    save = { saveableJson.encodeToString(it) },
    restore = { runCatching { saveableJson.decodeFromString<QuizSession>(it) }.getOrNull() }
)

/** 词条列表 ↔ JSON 字符串；空串表示「没有覆盖词表」（空列表会编码为 "[]"，两者不混淆）。 */
fun encodeWordList(list: List<WordEntry>): String = saveableJson.encodeToString(list)

fun decodeWordListOrNull(raw: String): List<WordEntry>? =
    if (raw.isEmpty()) null
    else runCatching { saveableJson.decodeFromString<List<WordEntry>>(raw) }.getOrNull()
