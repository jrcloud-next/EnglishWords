package com.jr.englishword.net

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ApiRequestTest {
    private fun request(
        endpointUrl: String = "https://api.deepseek.com/chat/completions",
        model: String = "deepseek-flash",
        task: AiTask? = AiTask.WORD_INFO,
        system: String = "词典规则",
        user: String = """{"word":"charge","pos":"n."}"""
    ): JsonObject = buildChatRequestBody(
        endpointUrl = endpointUrl,
        model = model,
        system = system,
        user = user,
        maxTokens = 2048,
        temperature = 0.3,
        task = task
    )

    @Test
    fun compatibleProviderKeepsExistingCompletePayloadAndFieldOrder() {
        val body = request(
            endpointUrl = "https://compatible.example/v1/chat/completions",
            model = "generic-model"
        )

        assertEquals(
            """{"model":"generic-model","stream":false,"temperature":0.3,"max_tokens":2048,"messages":[{"role":"system","content":"词典规则"},{"role":"user","content":"{\"word\":\"charge\",\"pos\":\"n.\"}"}]}""",
            body.toString()
        )
    }

    @Test
    fun officialFlashWordInfoAddsOnlyDisabledThinkingAfterStandardFields() {
        val standard = request(endpointUrl = "https://compatible.example/v1/chat/completions")
        val endpoints = listOf(
            "https://api.deepseek.com/chat/completions",
            "https://api.deepseek.com/v1/chat/completions",
            "https://api.deepseek.com:443/v1/chat/completions",
            "HTTPS://API.DEEPSEEK.COM/v1/chat/completions"
        )

        endpoints.forEach { endpoint ->
            val body = request(endpointUrl = endpoint)
            assertEquals(listOf("model", "stream", "temperature", "max_tokens", "messages", "thinking"),
                body.keys.toList())
            assertEquals(Json.parseToJsonElement("""{"type":"disabled"}"""), body.getValue("thinking"))
            assertEquals(standard, JsonObject(body.filterKeys { it != "thinking" }))
        }
    }

    @Test
    fun distractorsAndConnectionTestsKeepServiceDefaultThinking() {
        listOf<AiTask?>(AiTask.EN_CN, AiTask.CN_EN, null).forEach { task ->
            val body = request(task = task)
            assertFalse("task=$task", body.containsKey("thinking"))
        }
    }

    @Test
    fun distractorLimitsAndSamplingRemainCallerControlled() {
        val body = buildChatRequestBody(
            endpointUrl = "https://api.deepseek.com/chat/completions",
            model = "deepseek-flash",
            system = "干扰项规则",
            user = """{"direction":"CN_EN","word":"repair","pos":"v.","meaning":"修理"}""",
            maxTokens = 1024,
            temperature = 0.9,
            task = AiTask.CN_EN
        )

        assertEquals("1024", body.getValue("max_tokens").jsonPrimitive.content)
        assertEquals("0.9", body.getValue("temperature").jsonPrimitive.content)
        assertEquals("false", body.getValue("stream").jsonPrimitive.content)
        assertFalse(body.containsKey("thinking"))
    }

    @Test
    fun differentModelsDoNotReceiveProviderSpecificThinking() {
        listOf("generic-model", "deepseek-chat", "DeepSeek-Flash", "deepseek-flash ", " deepseek-flash")
            .forEach { model ->
                val body = request(model = model)
                assertEquals(model, body.getValue("model").jsonPrimitive.content)
                assertFalse("model=$model", body.containsKey("thinking"))
            }
    }

    @Test
    fun blankModelRetainsOriginalFallbackWithoutThinkingOverride() {
        listOf("", " \t\n").forEach { model ->
            val body = request(model = model)
            assertEquals("deepseek-chat", body.getValue("model").jsonPrimitive.content)
            assertFalse(body.containsKey("thinking"))
        }
    }

    @Test
    fun unofficialInsecureNonDefaultPortAndInvalidUrlsKeepStandardPayload() {
        val standard = request(endpointUrl = "https://compatible.example/v1/chat/completions")
        val endpoints = listOf(
            "https://compatible.example/v1/chat/completions",
            "http://api.deepseek.com/chat/completions",
            "http://api.deepseek.com:443/chat/completions",
            "https://api.deepseek.com:8443/chat/completions",
            "https://api.deepseek.com.evil.example/chat/completions",
            "https://other.api.deepseek.com/chat/completions",
            "https://api-deepseek.com/chat/completions",
            "https://api.deepseek.com@proxy.example/chat/completions",
            "https://proxy.example/chat/completions?target=https://api.deepseek.com",
            "https://127.0.0.1/chat/completions",
            "api.deepseek.com/chat/completions",
            "not a URL"
        )

        endpoints.forEach { endpoint ->
            assertEquals("endpoint=$endpoint", standard, request(endpointUrl = endpoint))
        }
    }

    @Test
    fun messageStringsAndNestedUserJsonRoundTripWithoutRewriting() {
        val system = "规则：\"保留原文\"\\路径\n下一行\t制表\r回车\b退格\u000C换页"
        val user = """{"word":"a\"b\\c\nword","pos":"n.\t"}"""
        val body = request(system = system, user = user)
        val decoded = Json.parseToJsonElement(body.toString()).jsonObject
        val messages = decoded.getValue("messages").jsonArray

        assertEquals(2, messages.size)
        assertEquals(listOf("system", "user"),
            messages.map { it.jsonObject.getValue("role").jsonPrimitive.content })
        assertEquals(system, messages[0].jsonObject.getValue("content").jsonPrimitive.content)
        assertEquals(user, messages[1].jsonObject.getValue("content").jsonPrimitive.content)
        val userData = Json.parseToJsonElement(messages[1].jsonObject.getValue("content").jsonPrimitive.content)
            .jsonObject
        assertEquals("a\"b\\c\nword", userData.getValue("word").jsonPrimitive.content)
        assertEquals("n.\t", userData.getValue("pos").jsonPrimitive.content)
        assertEquals("2048", decoded.getValue("max_tokens").jsonPrimitive.content)
        assertEquals("0.3", decoded.getValue("temperature").jsonPrimitive.content)
    }
}
