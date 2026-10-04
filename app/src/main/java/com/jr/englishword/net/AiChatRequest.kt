package com.jr.englishword.net

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** 构造 Chat Completions 请求，仅对已验证的官方释义模型关闭思考。 */
internal fun buildChatRequestBody(
    endpointUrl: String,
    model: String,
    system: String,
    user: String,
    maxTokens: Int,
    temperature: Double,
    task: AiTask?
): JsonObject {
    val endpoint = endpointUrl.toHttpUrlOrNull()
    val officialEndpoint = endpoint?.let {
        it.isHttps && it.host == "api.deepseek.com" && it.port == 443
    } == true
    val disableThinking = task == AiTask.WORD_INFO && officialEndpoint && model == "deepseek-flash"

    return buildJsonObject {
        put("model", model.ifBlank { "deepseek-chat" })
        put("stream", false)
        put("temperature", temperature)
        put("max_tokens", maxTokens)
        put("messages", buildJsonArray {
            add(buildJsonObject {
                put("role", "system")
                put("content", system)
            })
            add(buildJsonObject {
                put("role", "user")
                put("content", user)
            })
        })
        if (disableThinking) {
            put("thinking", buildJsonObject { put("type", "disabled") })
        }
    }
}
