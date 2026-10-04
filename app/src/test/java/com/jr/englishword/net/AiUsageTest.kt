package com.jr.englishword.net

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AiUsageTest {
    private fun parse(response: String): AiUsage? =
        parseAiUsage(Json.parseToJsonElement(response).jsonObject)

    @Test
    fun completeDeepSeekUsagePreservesReportedCounts() {
        val usage = parse(
            """{"usage":{"prompt_tokens":100,"completion_tokens":20,"prompt_cache_hit_tokens":64,"prompt_cache_miss_tokens":36}}"""
        )

        assertEquals(AiUsage(100, 20, 64, 36), usage)
        assertEquals(0.64, usage!!.cacheHitRate!!, 0.000001)
    }

    @Test
    fun absentOrNonObjectUsageIsNotReportedAsZero() {
        assertNull(parseAiUsage(null))
        assertNull(parse("{}"))
        listOf("null", "false", "12", "[]", "\"unavailable\"").forEach { value ->
            assertNull("usage=$value", parse("""{"usage":$value}"""))
        }
    }

    @Test
    fun emptyUsageObjectKeepsEveryCountUnknown() {
        val usage = parse("""{"usage":{}}""")

        assertNotNull(usage)
        assertEquals(AiUsage(), usage)
        assertNull(usage!!.cacheHitRate)
    }

    @Test
    fun partialUsageDoesNotInferMissingCounts() {
        val usage = parse("""{"usage":{"completion_tokens":12,"prompt_cache_hit_tokens":10}}""")

        assertEquals(AiUsage(completionTokens = 12, cacheHitTokens = 10), usage)
        assertNull(usage!!.cacheHitRate)
    }

    @Test
    fun explicitlyReportedZeroRemainsZero() {
        val usage = parse(
            """{"usage":{"prompt_tokens":0,"completion_tokens":0,"prompt_cache_hit_tokens":0,"prompt_cache_miss_tokens":0}}"""
        )

        assertEquals(AiUsage(0, 0, 0, 0), usage)
        assertNull(usage!!.cacheHitRate)
    }

    @Test
    fun invalidCountsRemainUnknownRatherThanBeingCoerced() {
        val invalidValues = listOf(
            "\"7\"", "7.0", "7e0", "true", "false", "-1",
            "9223372036854775808", "{}", "[]", "null"
        )
        invalidValues.forEach { value ->
            val usage = parse(
                """{"usage":{"prompt_tokens":$value,"completion_tokens":$value,"prompt_cache_hit_tokens":$value,"prompt_cache_miss_tokens":$value}}"""
            )

            assertEquals("count=$value", AiUsage(), usage)
        }
    }

    @Test
    fun malformedFieldDoesNotDiscardOtherValidCounts() {
        val usage = parse(
            """{"usage":{"prompt_tokens":100,"completion_tokens":"20","prompt_cache_hit_tokens":40,"prompt_cache_miss_tokens":60}}"""
        )

        assertEquals(AiUsage(100, null, 40, 60), usage)
        assertEquals(0.4, usage!!.cacheHitRate!!, 0.000001)
    }

    @Test
    fun largestNonNegativeLongCountsAreSupported() {
        val usage = parse(
            """{"usage":{"prompt_tokens":9223372036854775807,"completion_tokens":9223372036854775807,"prompt_cache_hit_tokens":9223372036854775807,"prompt_cache_miss_tokens":9223372036854775807}}"""
        )

        assertEquals(AiUsage(Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE), usage)
        assertEquals(1.0, usage!!.cacheHitRate!!, 0.0)
    }

    @Test
    fun compatibleCachedTokensAreUsedOnlyWhenDeepSeekHitKeyIsAbsent() {
        val usage = parse(
            """{"usage":{"prompt_tokens":100,"completion_tokens":20,"prompt_tokens_details":{"cached_tokens":40}}}"""
        )

        assertEquals(AiUsage(100, 20, 40, null), usage)
        assertEquals(0.4, usage!!.cacheHitRate!!, 0.000001)
    }

    @Test
    fun explicitDeepSeekZeroTakesPriorityOverCompatibleCount() {
        val usage = parse(
            """{"usage":{"prompt_tokens":100,"prompt_cache_hit_tokens":0,"prompt_tokens_details":{"cached_tokens":40}}}"""
        )

        assertEquals(AiUsage(promptTokens = 100, cacheHitTokens = 0), usage)
        assertEquals(0.0, usage!!.cacheHitRate!!, 0.0)
    }

    @Test
    fun malformedDeepSeekHitIsNotReplacedByCompatibleCount() {
        listOf("null", "\"40\"", "40.0", "true", "-1").forEach { value ->
            val usage = parse(
                """{"usage":{"prompt_tokens":100,"prompt_cache_hit_tokens":$value,"prompt_tokens_details":{"cached_tokens":40}}}"""
            )

            assertEquals("hit=$value", AiUsage(promptTokens = 100), usage)
            assertNull(usage!!.cacheHitRate)
        }
    }

    @Test
    fun malformedCompatibleUsageDoesNotInventCacheCounts() {
        listOf("null", "false", "[]", "{}", """{"cached_tokens":"40"}""", """{"cached_tokens":40.0}""").forEach { details ->
            assertEquals(
                "details=$details",
                AiUsage(promptTokens = 100),
                parse("""{"usage":{"prompt_tokens":100,"prompt_tokens_details":$details}}""")
            )
        }
    }

    @Test
    fun cacheHitRateIncludesValidLowerAndUpperBounds() {
        assertEquals(0.0, AiUsage(promptTokens = 100, cacheHitTokens = 0).cacheHitRate!!, 0.0)
        assertEquals(0.5, AiUsage(promptTokens = 100, cacheHitTokens = 50).cacheHitRate!!, 0.0)
        assertEquals(1.0, AiUsage(promptTokens = 100, cacheHitTokens = 100).cacheHitRate!!, 0.0)
    }

    @Test
    fun cacheHitRateIsUnknownWithoutValidDenominatorAndHitCount() {
        listOf(
            AiUsage(),
            AiUsage(promptTokens = 100),
            AiUsage(cacheHitTokens = 40),
            AiUsage(promptTokens = 0, cacheHitTokens = 0),
            AiUsage(promptTokens = -1, cacheHitTokens = 0),
            AiUsage(promptTokens = 100, cacheHitTokens = -1),
            AiUsage(promptTokens = 100, cacheHitTokens = 101)
        ).forEach { usage ->
            assertNull("usage=$usage", usage.cacheHitRate)
        }
    }
}
