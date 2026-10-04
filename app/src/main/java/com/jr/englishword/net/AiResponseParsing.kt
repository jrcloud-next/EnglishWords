package com.jr.englishword.net

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

private val responseJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

internal fun parseDistractors(content: String, correct: String): List<String> {
    val text = stripCodeFence(content)
    val start = text.indexOf('[')
    val end = text.lastIndexOf(']')
    if (start < 0 || end <= start) return emptyList()
    val candidates = runCatching {
        responseJson.parseToJsonElement(text.substring(start, end + 1))
            .jsonArray.map { it.jsonPrimitive.content }
    }.getOrDefault(emptyList())
    val normalizedCorrect = normalizeAnswer(correct)
    return candidates.filter { it.isNotBlank() && normalizeAnswer(it) != normalizedCorrect }
        .distinctBy { normalizeAnswer(it) }
        .take(3)
}

internal fun parseWordInfo(content: String): Api.AiInfoResult {
    val text = stripCodeFence(content)
    val start = text.indexOf('{')
    val end = text.lastIndexOf('}')
    if (start < 0 || end <= start) {
        return Api.AiInfoResult.Err("AI 返回格式无法解析")
    }
    return runCatching {
        responseJson.decodeFromString<Api.AiWordInfo>(text.substring(start, end + 1))
    }.fold(
        onSuccess = { Api.AiInfoResult.Ok(it) },
        onFailure = { Api.AiInfoResult.Err("AI 返回格式无法解析：${it.message}") }
    )
}

private fun stripCodeFence(content: String): String {
    var text = content.trim()
    if (text.startsWith("```")) {
        text = text.removePrefix("```json").removePrefix("```JSON").removePrefix("```")
        text = text.removeSuffix("```").trim()
    }
    return text
}

private fun normalizeAnswer(value: String): String =
    value.lowercase().filter { it.isLetterOrDigit() }
