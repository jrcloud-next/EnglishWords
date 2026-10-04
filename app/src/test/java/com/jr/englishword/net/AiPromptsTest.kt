package com.jr.englishword.net

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiPromptsTest {
    @Test
    fun distractorSystemStaysIdenticalAcrossWordsAndDirections() {
        val englishToChinese = distractorPrompt("apple", "n.", "苹果", true)
        val chineseToEnglish = distractorPrompt("develop", "v.", "发展", false)

        assertEquals(englishToChinese.system, chineseToEnglish.system)
        assertNotEquals(englishToChinese.user, chineseToEnglish.user)
        assertFalse(englishToChinese.system.contains("apple"))
        assertFalse(englishToChinese.system.contains("苹果"))
        assertFalse(chineseToEnglish.system.contains("develop"))
        assertFalse(chineseToEnglish.system.contains("发展"))
    }

    @Test
    fun distractorExamplesStayInSharedSystemInsteadOfUserInput() {
        val englishToChinese = distractorPrompt("eager", "adj.", "热切的", true)
        val chineseToEnglish = distractorPrompt("repair", "v.", "修理", false)
        assertEquals(englishToChinese.system, chineseToEnglish.system)

        val exampleInputs = englishToChinese.system.lineSequence()
            .filter { it.startsWith("输入：") }
            .map { Json.parseToJsonElement(it.removePrefix("输入：")).jsonObject }
            .toList()
        val exampleOutputs = englishToChinese.system.lineSequence()
            .filter { it.startsWith("输出：") }
            .map { Json.parseToJsonElement(it.removePrefix("输出：")).jsonArray }
            .toList()

        assertEquals(listOf("absorb", "gather"),
            exampleInputs.map { it.getValue("word").jsonPrimitive.content })
        assertEquals(listOf("EN_CN", "CN_EN"),
            exampleInputs.map { it.getValue("direction").jsonPrimitive.content })
        assertEquals(listOf("v.", "v."),
            exampleInputs.map { it.getValue("pos").jsonPrimitive.content })
        assertEquals(2, exampleOutputs.size)
        exampleOutputs.forEach { output ->
            val choices = output.map { it.jsonPrimitive.content }
            assertEquals(3, choices.size)
            assertEquals(3, choices.distinct().size)
        }

        listOf(englishToChinese to "eager", chineseToEnglish to "repair").forEach { (prompt, word) ->
            val user = Json.parseToJsonElement(prompt.user).jsonObject
            assertEquals(listOf("direction", "word", "pos", "meaning"), user.keys.toList())
            assertEquals(word, user.getValue("word").jsonPrimitive.content)
            exampleInputs.forEach { example ->
                assertFalse(prompt.user.contains(example.getValue("word").jsonPrimitive.content))
            }
        }
    }

    @Test
    fun chineseToEnglishUsesRealMeaningAndPartOfSpeech() {
        val data = Json.parseToJsonElement(
            distractorPrompt("record", "v.", "记录；记载", false).user
        ).jsonObject

        assertEquals(listOf("direction", "word", "pos", "meaning"), data.keys.toList())
        assertEquals("CN_EN", data.getValue("direction").jsonPrimitive.content)
        assertEquals("record", data.getValue("word").jsonPrimitive.content)
        assertEquals("v.", data.getValue("pos").jsonPrimitive.content)
        assertEquals("记录；记载", data.getValue("meaning").jsonPrimitive.content)
    }

    @Test
    fun englishToChineseKeepsDirectionAndEscapesDynamicData() {
        val word = "a\"b\\c\nword"
        val pos = "n.\t/ v."
        val meaning = "他说\"你好\"；路径\\目录\n下一行"
        val data = Json.parseToJsonElement(distractorPrompt(word, pos, meaning, true).user).jsonObject

        assertEquals("EN_CN", data.getValue("direction").jsonPrimitive.content)
        assertEquals(word, data.getValue("word").jsonPrimitive.content)
        assertEquals(pos, data.getValue("pos").jsonPrimitive.content)
        assertEquals(meaning, data.getValue("meaning").jsonPrimitive.content)
    }

    @Test
    fun wordInfoHasItsOwnStableSystemAndEscapedInput() {
        val first = wordInfoPrompt("Mr. \"Smith\"\\\n", "n.\t")
        val second = wordInfoPrompt("develop", "v.")
        val data = Json.parseToJsonElement(first.user).jsonObject

        assertEquals(first.system, second.system)
        assertNotEquals(first.system, distractorPrompt("apple", "n.", "苹果", true).system)
        assertFalse(first.system.contains("Smith"))
        assertEquals(listOf("word", "pos"), data.keys.toList())
        assertEquals("Mr. \"Smith\"\\\n", data.getValue("word").jsonPrimitive.content)
        assertEquals("n.\t", data.getValue("pos").jsonPrimitive.content)
    }

    @Test
    fun wordInfoUsesShortRulesWithoutBusinessExamples() {
        val first = wordInfoPrompt("charge", "n.")
        val second = wordInfoPrompt("calm", "adj.")
        assertEquals(first.system, second.system)
        assertFalse(first.system.contains("\"direction\":"))
        assertFalse(first.system.contains("charge"))
        assertFalse(first.system.contains("calm"))

        assertFalse(first.system.contains("输入："))
        assertFalse(first.system.contains("输出："))
        assertFalse(first.system.contains("light"))
        assertTrue(first.system.contains("所有常见义项，最多5条"))
        val format = first.system.substringAfter("只输出JSON对象：").substringBefore("，不要输出")
        val fields = Json.parseToJsonElement(format).jsonObject
        assertEquals(listOf("senses", "example", "exampleCn"), fields.keys.toList())
        assertEquals(listOf("pos", "meaning"), fields.getValue("senses").jsonArray.first().jsonObject.keys.toList())

        listOf(first to "charge", second to "calm").forEach { (prompt, word) ->
            val user = Json.parseToJsonElement(prompt.user).jsonObject
            assertEquals(listOf("word", "pos"), user.keys.toList())
            assertEquals(word, user.getValue("word").jsonPrimitive.content)
        }
    }

    @Test
    fun optionalPartOfSpeechIsStillPresentWhenEmpty() {
        val distractorData = Json.parseToJsonElement(
            distractorPrompt("ice cream", "", "冰淇淋", true).user
        ).jsonObject
        val infoData = Json.parseToJsonElement(wordInfoPrompt("ice cream", "").user).jsonObject

        assertEquals("", distractorData.getValue("pos").jsonPrimitive.content)
        assertEquals("", infoData.getValue("pos").jsonPrimitive.content)
    }
}
