package com.jr.englishword.data

import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction
import java.util.UUID
import java.util.zip.ZipInputStream

/**
 * 单词行解析：兼容 "1. hello  n. 你好"、"hello n. 你好"、"hello  你好" 等格式。
 * 另含 docx 正文提取与 txt 编码自适应解码。
 */
object Parser {

    data class ParseResult(val words: List<WordEntry>, val failedLines: List<String>)

    private const val DIGITS = "0-9０-９"

    private val POS_SET =
        "vt|vi|adj|adv|prep|conj|pron|interj|int|art|aux|num|abbr|pl|phrase|n|v"

    /** 括号序号：（1）/ (1) */
    private val NUM_PAREN = Regex("""^[（(]\s*[$DIGITS]{1,4}\s*[)）]\s*""")

    /** 序号 + 分隔符：`1.` / `1、` / `1．`；后接数字时不视为序号，避免切坏 `1.5` 这类词。 */
    private val NUM_DOT = Regex("""^[$DIGITS]{1,4}\s*[\.、．]\s*(?![$DIGITS])""")

    /**
     * 裸序号（无分隔符）：仅当其后既不是词性标记、也不是非 ASCII 开头时才剥离。
     * 否则 "2024 n. 年份"、"10 n. 十" 这类以数字开头的单词会被误当序号切掉首数字。
     */
    private val NUM_BARE = Regex(
        """^[$DIGITS]{1,4}\s+(?!(?:$POS_SET)\s*[\.、．\s])(?=[A-Za-z0-9])""",
        RegexOption.IGNORE_CASE
    )

    /** 词头允许多词、数字与内部点号，以覆盖 "3D"、"U.S."、"Mr. Smith" 这类写法。 */
    private val LINE_POS = Regex(
        """^(?<word>[A-Za-z0-9][A-Za-z0-9'’.\-]*(?:\s[A-Za-z0-9'’.\-]+)*)\s+(?<pos>$POS_SET)\s*\.\s*(?<rest>\S.*)$""",
        RegexOption.IGNORE_CASE
    )

    /** 行尾词性，允许带结尾句点。 */
    private val TRAILING_POS = Regex("""\s+($POS_SET)\.?$""", RegexOption.IGNORE_CASE)

    /** 兜底分支用的纯词头形状。 */
    private val BARE_WORD = Regex("""[A-Za-z0-9][A-Za-z0-9'’.\- ]*""")

    private val MULTI_SPACE = Regex("""\s{2,}""")

    private fun isCjk(c: Char): Boolean =
        c in '\u4E00'..'\u9FFF' || c in '\u3400'..'\u4DBF'

    private fun firstCjkIndex(s: String): Int = s.indexOfFirst { isCjk(it) }

    private fun newEntry(word: String, pos: String, meaning: String): WordEntry? {
        val w = word.trim()
        val m = meaning.trim().trimStart('.', '．', '、', '，', ',', '　')
        if (w.isEmpty() || m.isEmpty()) return null
        return WordEntry(id = UUID.randomUUID().toString(), word = w, pos = pos.trim(), meaning = m)
    }

    /** 依次尝试三种序号写法，命中即剥离；都未命中时原样返回。 */
    private fun stripIndexPrefix(line: String): String {
        NUM_PAREN.replaceFirst(line, "").let { if (it != line) return it.trim() }
        NUM_DOT.replaceFirst(line, "").let { if (it != line) return it.trim() }
        NUM_BARE.replaceFirst(line, "").let { if (it != line) return it.trim() }
        return line
    }

    fun parseLine(raw: String): WordEntry? {
        var line = raw.trim()
        if (line.isEmpty()) return null
        line = stripIndexPrefix(line)
        if (line.isEmpty()) return null

        LINE_POS.find(line)?.let { m ->
            val word = m.groups["word"]?.value ?: return@let
            val pos = (m.groups["pos"]?.value ?: "") + "."
            val rest = m.groups["rest"]?.value ?: ""
            return newEntry(word, pos, rest)
        }

        // 兜底 1：按两个以上空白拆分
        val parts = line.split(MULTI_SPACE).map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size >= 2 && parts[0].matches(BARE_WORD)) {
            return newEntry(parts[0], "", parts.drop(1).joinToString("  "))
        }

        // 兜底 2：按第一个中文字符切分
        val idx = firstCjkIndex(line)
        if (idx > 0) {
            val left = line.substring(0, idx).trim()
            val right = line.substring(idx).trim()
            var word = left
            var pos = ""
            TRAILING_POS.find(left)?.let { pm ->
                word = left.substring(0, pm.range.first).trim()
                pos = pm.groupValues[1] + "."
            }
            return newEntry(word, pos, right)
        }
        return null
    }

    fun parseText(text: String): ParseResult {
        val words = LinkedHashMap<String, WordEntry>()
        val failed = mutableListOf<String>()
        text.lineSequence().forEach { raw ->
            val t = raw.trim()
            if (t.isEmpty()) return@forEach
            val e = parseLine(t)
            if (e == null) {
                failed.add(t)
            } else {
                val key = e.word.lowercase() + "|" + e.pos.lowercase()
                if (!words.containsKey(key)) words[key] = e
            }
        }
        return ParseResult(words.values.toList(), failed)
    }

    /** 从 docx（zip）中提取 word/document.xml 的纯文本，段落间以换行分隔。 */
    fun extractDocxText(bytes: ByteArray): String {
        val zin = ZipInputStream(ByteArrayInputStream(bytes))
        var xml: String? = null
        try {
            var entry = zin.nextEntry
            while (entry != null && xml == null) {
                if (entry.name == "word/document.xml") {
                    xml = zin.readBytes().toString(Charsets.UTF_8)
                }
                entry = zin.nextEntry
            }
        } finally {
            zin.close()
        }
        val doc = xml ?: throw IllegalArgumentException("不是有效的 docx 文件（缺少正文）")

        val sb = StringBuilder()
        for (para in doc.split("</w:p>")) {
            // 制表符/换行符占位转为空格，避免前后文本粘连
            val p = para
                .replace(Regex("""<w:tab[^>]*/?>"""), " ")
                .replace(Regex("""<w:br[^>]*/?>"""), " ")
            for (t in Regex("""<w:t[^>]*>(.*?)</w:t>""", RegexOption.DOT_MATCHES_ALL).findAll(p)) {
                sb.append(unescapeXml(t.groupValues[1]))
            }
            sb.append('\n')
        }
        return sb.toString()
    }

    private fun unescapeXml(s: String): String = s
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")

    /** txt 解码：BOM 优先，UTF-8 严格校验失败则回退 GBK。 */
    fun decodeBytes(bytes: ByteArray): String {
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            return String(bytes, 3, bytes.size - 3, Charsets.UTF_8)
        }
        if (bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte()) {
            return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
        }
        try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            return decoder.decode(ByteBuffer.wrap(bytes)).toString()
        } catch (e: CharacterCodingException) {
            return String(bytes, charset("GBK"))
        }
    }
}
