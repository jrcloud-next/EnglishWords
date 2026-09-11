package com.jr.englishword.net

import com.jr.englishword.data.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** OpenAI 兼容 Chat Completions 客户端（默认 DeepSeek），并提供 AI 扩展能力。 */
object DeepSeekApi {

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

    fun endpoint(baseUrl: String): String {
        var b = baseUrl.trim()
        if (!b.startsWith("http://") && !b.startsWith("https://")) b = "https://$b"
        b = b.trimEnd('/')
        return if (b.endsWith("/chat/completions")) b else "$b/chat/completions"
    }

    /** 三项 API 配置是否完整。 */
    fun isConfigured(settings: AppSettings): Boolean =
        settings.baseUrl.isNotBlank() && settings.model.isNotBlank() && settings.apiKey.isNotBlank()

    sealed class ApiResult {
        data class Ok(val content: String, val latencyMs: Long) : ApiResult()
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
        temperature: Double = 0.8
    ): ApiResult = withContext(Dispatchers.IO) {
        if (!isConfigured(settings)) {
            return@withContext ApiResult.Err("请先在设置中配置 API 地址、模型名称和 API Key")
        }
        val started = System.currentTimeMillis()
        try {
            val body = buildJsonObject {
                put("model", settings.model.ifBlank { "deepseek-chat" })
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
            }
            val req = Request.Builder()
                .url(endpoint(settings.baseUrl))
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
                val content = runCatching {
                    json.parseToJsonElement(text).jsonObject["choices"]?.jsonArray
                        ?.firstOrNull()?.jsonObject?.get("message")
                        ?.jsonObject?.get("content")?.jsonPrimitive?.content
                }.getOrNull()
                if (content.isNullOrBlank()) ApiResult.Err("接口返回内容为空")
                else ApiResult.Ok(content, System.currentTimeMillis() - started)
            }
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
        correct: String,
        chinese: Boolean
    ): List<String> {
        if (!settings.apiEnabled || !isConfigured(settings)) return emptyList()
        val withPos = if (pos.isNotBlank()) "$word ($pos)" else word
        val user = if (chinese) {
            "英语单词「$withPos」的正确中文释义是「$correct」。请生成3个错误的中文释义作为四选一选择题的干扰项。" +
                "要求：词性与正确释义一致、长度相近、但含义明显不同。只输出JSON数组，" +
                "格式如 [\"干扰项1\",\"干扰项2\",\"干扰项3\"]，不要输出其他任何内容。"
        } else {
            "中文释义「$correct」对应的英文单词是「${word.trim()}」。请生成3个错误的英文单词作为四选一选择题的干扰项。" +
                "要求：与正确单词词性相同、长度或拼写有一定相似、但含义不同且真实存在。只输出JSON数组，" +
                "格式如 [\"word1\",\"word2\",\"word3\"]，不要输出其他任何内容。"
        }
        val r = chat(
            settings,
            system = "你是一名英语词汇老师，为选择题生成干扰项。只输出JSON，不要任何解释。",
            user = user,
            maxTokens = 1024,
            temperature = 0.9
        )
        if (r !is ApiResult.Ok) return emptyList()
        val list = extractJsonArray(r.content)
        val normCorrect = normalize(correct)
        return list.filter { it.isNotBlank() && normalize(it) != normCorrect }
            .distinctBy { normalize(it) }
            .take(3)
    }

    private fun normalize(s: String): String =
        s.lowercase().filter { it.isLetterOrDigit() }

    private fun extractJsonArray(content: String): List<String> {
        var s = content.trim()
        if (s.startsWith("```")) {
            s = s.removePrefix("```json").removePrefix("```JSON").removePrefix("```")
            s = s.removeSuffix("```").trim()
        }
        val start = s.indexOf('[')
        val end = s.lastIndexOf(']')
        if (start < 0 || end <= start) return emptyList()
        return runCatching {
            json.parseToJsonElement(s.substring(start, end + 1))
                .jsonArray.map { it.jsonPrimitive.content }
        }.getOrDefault(emptyList())
    }

    /** 获取某单词的 AI 详细释义（多义项 + 例句）。 */
    suspend fun moreInfo(settings: AppSettings, word: String, pos: String): AiInfoResult {
        val withPos = if (pos.isNotBlank()) "$word ($pos)" else word
        val user = "请像权威英汉词典一样，给出英语单词「$withPos」的详细信息：" +
            "senses 为该词所有常见义项（pos 为词性如 n./v./adj.，meaning 为简明中文释义，最多5条）；" +
            "example 为一句常用英文例句，exampleCn 为其对应中文翻译。" +
            "只输出JSON：{\"senses\":[{\"pos\":\"n.\",\"meaning\":\"...\"}],\"example\":\"...\",\"exampleCn\":\"...\"}"
        val r = chat(
            settings,
            system = "你是权威英汉词典，只输出JSON，不要任何解释和多余文本。",
            user = user,
            maxTokens = 2048,
            temperature = 0.3
        )
        return when (r) {
            is ApiResult.Err -> AiInfoResult.Err(r.message)
            is ApiResult.Ok -> {
                var s = r.content.trim()
                if (s.startsWith("```")) {
                    s = s.removePrefix("```json").removePrefix("```JSON").removePrefix("```")
                    s = s.removeSuffix("```").trim()
                }
                val start = s.indexOf('{')
                val end = s.lastIndexOf('}')
                if (start < 0 || end <= start) {
                    AiInfoResult.Err("AI 返回格式无法解析")
                } else {
                    runCatching {
                        json.decodeFromString<AiWordInfo>(s.substring(start, end + 1))
                    }.fold(
                        onSuccess = { AiInfoResult.Ok(it) },
                        onFailure = { AiInfoResult.Err("AI 返回格式无法解析：${it.message}") }
                    )
                }
            }
        }
    }
}
