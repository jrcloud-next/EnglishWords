package com.jr.englishword.net

import com.jr.englishword.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/** OpenAI 兼容 Chat Completions 客户端，并提供 AI 扩展能力。 */
object Api {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _lastBusinessUsage = MutableStateFlow<AiUsageSnapshot?>(null)
    val lastBusinessUsage: StateFlow<AiUsageSnapshot?> = _lastBusinessUsage.asStateFlow()

    fun endpoint(baseUrl: String): String {
        var b = baseUrl.trim()
        if (!b.startsWith("http://") && !b.startsWith("https://")) b = "https://$b"
        b = b.trimEnd('/')
        return if (b.endsWith("/chat/completions")) b else "$b/chat/completions"
    }

    /** 三项 API 配置是否完整。 */
    fun isConfigured(settings: AppSettings): Boolean =
        settings.baseUrl.isNotBlank() && settings.model.isNotBlank() && settings.apiKey.isNotBlank()

    /** AI 是否已开启且配置完整。全应用唯一的「AI 可用」判定，避免多处各写一遍。 */
    fun isReady(settings: AppSettings): Boolean =
        settings.apiEnabled && isConfigured(settings)

    sealed class ApiResult {
        data class Ok(val content: String, val latencyMs: Long, val usage: AiUsage? = null) : ApiResult()
        data class Err(val message: String) : ApiResult()
    }

    @Serializable
    data class Sense(val pos: String = "", val meaning: String = "")

    @Serializable
    data class AiWordInfo(
        val senses: List<Sense> = emptyList(),
        val example: String = "",
        val exampleCn: String = ""
    )

    sealed class AiInfoResult {
        data class Ok(val info: AiWordInfo) : AiInfoResult()
        data class Err(val message: String) : AiInfoResult()
    }

    suspend fun chat(
        settings: AppSettings,
        system: String,
        user: String,
        maxTokens: Int = 800,
        temperature: Double = 0.8,
        task: AiTask? = null
    ): ApiResult = withContext(Dispatchers.IO) {
        if (!isConfigured(settings)) {
            return@withContext ApiResult.Err("请先在设置中配置 API 地址、模型名称和 API Key")
        }
        val started = System.currentTimeMillis()
        try {
            val endpointUrl = endpoint(settings.baseUrl)
            val body = buildChatRequestBody(
                endpointUrl, settings.model, system, user, maxTokens, temperature, task
            )
            val req = Request.Builder()
                .url(endpointUrl)
                .header("Authorization", "Bearer ${settings.apiKey.trim()}")
                .post(body.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            client.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val errMsg = runCatching {
                        json.parseToJsonElement(text).jsonObject["error"]
                            ?.jsonObject?.get("message")?.jsonPrimitive?.content
                    }.getOrNull()
                    return@withContext ApiResult.Err("HTTP ${resp.code}${errMsg?.let { "：$it" } ?: ""}")
                }
                val response = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull()
                val content = runCatching {
                    response?.get("choices")?.jsonArray
                        ?.firstOrNull()?.jsonObject?.get("message")
                        ?.jsonObject?.get("content")?.jsonPrimitive?.content
                }.getOrNull()
                if (content.isNullOrBlank()) {
                    ApiResult.Err("接口返回内容为空")
                } else {
                    val usage = parseAiUsage(response)
                    val completedAtMs = System.currentTimeMillis()
                    if (task != null) {
                        val snapshot = AiUsageSnapshot(task, req.url.host, settings.model, completedAtMs, usage)
                        // 并发响应可能交错发布，避免较早完成的响应覆盖更新的统计。
                        _lastBusinessUsage.update { previous ->
                            if (previous == null || snapshot.completedAtMs >= previous.completedAtMs) snapshot
                            else previous
                        }
                    }
                    ApiResult.Ok(content, completedAtMs - started, usage)
                }
            }
        } catch (e: CancellationException) {
            // 协程取消必须向上传播，不能被当成普通网络错误吞掉
            throw e
        } catch (e: Exception) {
            ApiResult.Err(e.message ?: e.javaClass.simpleName)
        }
    }

    suspend fun testConnection(settings: AppSettings): ApiResult =
        chat(
            settings,
            system = "你是连通性测试助手，只输出OK。",
            user = "请只回复：OK",
            maxTokens = 256,
            temperature = 0.0
        )

    /** 为四选一生成 3 个干扰项；chinese=true 生成中文释义干扰项，否则生成英文单词干扰项。 */
    suspend fun aiDistractors(
        settings: AppSettings,
        word: String,
        pos: String,
        meaning: String,
        chinese: Boolean
    ): List<String> {
        if (!isReady(settings)) return emptyList()
        val prompt = distractorPrompt(word, pos, meaning, chinese)
        val r = chat(
            settings,
            system = prompt.system,
            user = prompt.user,
            maxTokens = 1024,
            temperature = 0.9,
            task = if (chinese) AiTask.EN_CN else AiTask.CN_EN
        )
        if (r !is ApiResult.Ok) return emptyList()
        return parseDistractors(r.content, if (chinese) meaning else word)
    }

    /** 获取某单词的 AI 详细释义（多义项 + 例句）。 */
    suspend fun moreInfo(settings: AppSettings, word: String, pos: String): AiInfoResult {
        val prompt = wordInfoPrompt(word, pos)
        val r = chat(
            settings,
            system = prompt.system,
            user = prompt.user,
            maxTokens = 2048,
            temperature = 0.3,
            task = AiTask.WORD_INFO
        )
        return when (r) {
            is ApiResult.Err -> AiInfoResult.Err(r.message)
            is ApiResult.Ok -> parseWordInfo(r.content)
        }
    }
}
