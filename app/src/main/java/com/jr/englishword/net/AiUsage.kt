package com.jr.englishword.net

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** 服务端返回的用量；未知计数保持 null，不将缺失字段当成零。 */
data class AiUsage(
    val promptTokens: Long? = null,
    val completionTokens: Long? = null,
    val cacheHitTokens: Long? = null,
    val cacheMissTokens: Long? = null
) {
    /** 本次请求缓存命中的输入占比；分母或计数无效时无法计算。 */
    val cacheHitRate: Double?
        get() {
            val prompt = promptTokens ?: return null
            val hit = cacheHitTokens ?: return null
            return if (prompt > 0 && hit in 0..prompt) hit.toDouble() / prompt else null
        }
}

enum class AiTask(val displayName: String) {
    EN_CN("英译中干扰项"),
    CN_EN("中译英干扰项"),
    WORD_INFO("详细释义")
}

/** 仅在内存中保留最近一次成功业务响应的元数据，不包含密钥、词条或响应正文。 */
data class AiUsageSnapshot(
    val task: AiTask,
    val serviceHost: String,
    val model: String,
    val completedAtMs: Long,
    val usage: AiUsage?
)

internal fun parseAiUsage(response: JsonObject?): AiUsage? {
    val usage = response?.get("usage") as? JsonObject ?: return null
    val hit = if ("prompt_cache_hit_tokens" in usage) {
        usage.nonNegativeCount("prompt_cache_hit_tokens")
    } else {
        (usage["prompt_tokens_details"] as? JsonObject)?.nonNegativeCount("cached_tokens")
    }
    return AiUsage(
        promptTokens = usage.nonNegativeCount("prompt_tokens"),
        completionTokens = usage.nonNegativeCount("completion_tokens"),
        cacheHitTokens = hit,
        cacheMissTokens = usage.nonNegativeCount("prompt_cache_miss_tokens")
    )
}

private fun JsonObject.nonNegativeCount(key: String): Long? {
    val value = this[key] as? JsonPrimitive ?: return null
    if (value.isString) return null
    // JsonPrimitive.longOrNull 会接受 7e0 等指数形式，这里只接受整数计数。
    return value.content.toLongOrNull()?.takeIf { it >= 0 }
}
