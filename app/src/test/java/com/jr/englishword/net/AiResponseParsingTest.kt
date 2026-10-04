package com.jr.englishword.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiResponseParsingTest {
    @Test
    fun distractorsExcludeCorrectAnswerDuplicatesAndBlankValues() {
        val content = """["Ice cream", "ICE-CREAM", "  ", "Tea", "tea!", "coffee", "milk", "water"]"""

        assertEquals(listOf("Tea", "coffee", "milk"), parseDistractors(content, "ice-cream"))
    }

    @Test
    fun distractorsSupportExistingFenceVariantsAndSurroundingText() {
        val variants = listOf(
            "[\"tea\",\"coffee\",\"milk\"]",
            "```json\n[\"tea\",\"coffee\",\"milk\"]\n```",
            "```JSON\n[\"tea\",\"coffee\",\"milk\"]\n```",
            "```\n[\"tea\",\"coffee\",\"milk\"]\n```",
            "以下是结果：[\"tea\",\"coffee\",\"milk\"]。"
        )

        variants.forEach { content ->
            assertEquals(listOf("tea", "coffee", "milk"), parseDistractors(content, "water"))
        }
    }

    @Test
    fun invalidDistractorResponsesFallBackToEmptyList() {
        listOf("", "not json", "[]", "[\"tea\"", "[{\"word\":\"tea\"}]").forEach { content ->
            assertTrue(parseDistractors(content, "water").isEmpty())
        }
    }

    @Test
    fun distractorsRetainExistingLenientPrimitiveParsing() {
        assertEquals(listOf("tea", "7"), parseDistractors("[tea,7]", "water"))
    }

    @Test
    fun distractorsPreserveOriginalCandidateTextAndCanReturnFewerThanThree() {
        assertEquals(listOf(" tea "), parseDistractors("[\" tea \",\"tea\",\"water\"]", "water"))
    }

    @Test
    fun wordInfoSupportsExistingFenceVariantsAndIgnoresUnknownFields() {
        val content = """{"senses":[{"pos":"n.","meaning":"苹果","extra":"ignored"}],"example":"An apple.","exampleCn":"一个苹果。","extra":true}"""
        val expected = Api.AiWordInfo(
            senses = listOf(Api.Sense("n.", "苹果")),
            example = "An apple.",
            exampleCn = "一个苹果。"
        )
        val variants = listOf(content, "```json\n$content\n```", "```JSON\n$content\n```", "```\n$content\n```", "结果：$content。")

        variants.forEach { response ->
            assertEquals(Api.AiInfoResult.Ok(expected), parseWordInfo(response))
        }
    }

    @Test
    fun wordInfoRetainsDefaultsForMissingOptionalFields() {
        assertEquals(Api.AiInfoResult.Ok(Api.AiWordInfo()), parseWordInfo("{}"))
        assertEquals(
            Api.AiInfoResult.Ok(Api.AiWordInfo(senses = listOf(Api.Sense()))),
            parseWordInfo("{\"senses\":[{}]}")
        )
    }

    @Test
    fun wordInfoWithoutObjectKeepsExistingErrorMessage() {
        assertEquals(Api.AiInfoResult.Err("AI 返回格式无法解析"), parseWordInfo("[]"))
        assertEquals(Api.AiInfoResult.Err("AI 返回格式无法解析"), parseWordInfo("not json"))
    }

    @Test
    fun malformedWordInfoKeepsExistingErrorPrefix() {
        val invalidJson = parseWordInfo("{\"senses\":[}")
        val invalidSchema = parseWordInfo("{\"senses\":\"not an array\"}")

        assertTrue(invalidJson is Api.AiInfoResult.Err)
        assertTrue(invalidSchema is Api.AiInfoResult.Err)
        assertTrue((invalidJson as Api.AiInfoResult.Err).message.startsWith("AI 返回格式无法解析："))
        assertTrue((invalidSchema as Api.AiInfoResult.Err).message.startsWith("AI 返回格式无法解析："))
    }
}
