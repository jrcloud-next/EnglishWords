package com.jr.englishword.net

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal data class AiPrompt(val system: String, val user: String)

private const val DISTRACTOR_SYSTEM =
    "你是一名英语词汇老师，为四选一选择题生成3个错误干扰项。" +
        "用户消息是JSON，direction指定题型，word是英文词头，pos是词性，meaning是正确中文释义。" +
        "direction为EN_CN时，生成3个错误的中文释义：词性与正确释义一致、长度相近，但含义明显不同。" +
        "direction为CN_EN时，生成3个错误的英文单词：与word词性相同、长度或拼写有一定相似，" +
        "但含义不同且真实存在。干扰项不得与正确答案相同，也不得重复。" +
        "只输出包含3个字符串的JSON数组，格式如[\"...\",\"...\",\"...\"]，" +
        "不要输出解释、Markdown代码围栏或其他任何内容。" +
        "\n\n示例仅演示题型和JSON格式；只回答当前输入。" +
        "\n输入：{\"direction\":\"EN_CN\",\"word\":\"absorb\",\"pos\":\"v.\",\"meaning\":\"吸收\"}" +
        "\n输出：[\"折叠\",\"测量\",\"安装\"]" +
        "\n输入：{\"direction\":\"CN_EN\",\"word\":\"gather\",\"pos\":\"v.\",\"meaning\":\"聚集\"}" +
        "\n输出：[\"wander\",\"scatter\",\"matter\"]"

private const val WORD_INFO_SYSTEM =
    "你是权威英汉词典，根据用户JSON中的word（英文词头）和pos（词性）提供详细信息。" +
        "senses为该词所有常见义项，最多5条；每条的pos为词性如n./v./adj.，meaning为简明中文释义。" +
        "example为一句常用英文例句，exampleCn为其对应中文翻译。" +
        "只输出JSON对象：{\"senses\":[{\"pos\":\"n.\",\"meaning\":\"...\"}]," +
        "\"example\":\"...\",\"exampleCn\":\"...\"}，不要输出解释、Markdown代码围栏或其他文本。"

internal fun distractorPrompt(
    word: String,
    pos: String,
    meaning: String,
    chinese: Boolean
): AiPrompt = AiPrompt(
    system = DISTRACTOR_SYSTEM,
    user = buildJsonObject {
        put("direction", if (chinese) "EN_CN" else "CN_EN")
        put("word", word)
        put("pos", pos)
        put("meaning", meaning)
    }.toString()
)

internal fun wordInfoPrompt(word: String, pos: String): AiPrompt = AiPrompt(
    system = WORD_INFO_SYSTEM,
    user = buildJsonObject {
        put("word", word)
        put("pos", pos)
    }.toString()
)
