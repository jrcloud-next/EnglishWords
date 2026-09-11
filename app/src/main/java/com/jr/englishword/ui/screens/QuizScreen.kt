package com.jr.englishword.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jr.englishword.data.AppSettings
import com.jr.englishword.data.QuizMode
import com.jr.englishword.data.WordEntry
import com.jr.englishword.ui.AppViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

private val GREEN = Color(0xFF16A34A)

data class Question(
    val word: WordEntry,
    val options: List<String> = emptyList(),
    val correctIndex: Int = -1
)

/** 每道题的作答状态，支持在题目间前进/后退时还原。 */
data class AnswerState(
    val selected: Int = -1,
    val typed: String = "",
    val answered: Boolean = false,
    val correct: Boolean = false
)

fun normalizeZh(s: String): String = s.lowercase().filter {
    !it.isWhitespace() && it !in "。，、；：！？!?,.;:()（）「」『』·~－—-_'\"“”‘’　"
}

fun normalizeEn(s: String): String = s.lowercase().filter { it.isLetterOrDigit() }

fun checkZhAnswer(input: String, meaning: String): Boolean {
    val i = normalizeZh(input)
    if (i.isEmpty()) return false
    val senses = meaning.split('；', ';', '，', ',', '。', '/', '／')
        .map { normalizeZh(it) }
        .filter { it.isNotEmpty() } + normalizeZh(meaning)
    if (senses.any { it == i }) return true
    return senses.any { (it.contains(i) && i.length >= 2) || (i.contains(it) && it.length >= 2) }
}

private val FALLBACK_CN = listOf(
    "重要的", "发展", "提供", "增加", "减少", "变化", "影响", "研究", "计划", "社会",
    "经济", "文化", "教育", "环境", "技术", "问题", "方法", "结果", "原因", "目标",
    "种类", "程度", "范围", "数量", "质量", "特点", "状态", "行为", "思想", "观点",
    "能力", "机会", "价值", "材料", "过程"
)

private val FALLBACK_EN = listOf(
    "time", "people", "way", "water", "word", "place", "case", "point", "group", "number",
    "fact", "hand", "right", "study", "world", "area", "part", "child", "woman", "week",
    "school", "family", "money", "story", "month", "book", "line", "order", "night", "life",
    "house", "friend", "father", "power", "question", "company", "system", "program",
    "answer", "lesson"
)

private suspend fun buildOptions(
    correct: String,
    pool: List<String>,
    settings: AppSettings,
    word: WordEntry,
    chinese: Boolean,
    vm: AppViewModel
): Pair<List<String>, Boolean> {
    val norm: (String) -> String = if (chinese) ::normalizeZh else ::normalizeEn

    // 干扰项优先全部由 AI 生成
    var distractors = if (settings.apiEnabled) vm.aiDistractors(word, chinese) else emptyList()
    val usedAi = distractors.isNotEmpty()

    // AI 未配置/失败时回退：先词库，再内置列表
    if (distractors.size < 3) {
        val fromBank = pool.filter { norm(it) != norm(correct) }
            .distinctBy { norm(it) }
            .filter { d -> distractors.none { norm(d) == norm(it) } }
            .shuffled()
        distractors = (distractors + fromBank).take(3)
    }
    if (distractors.size < 3) {
        val fallback = (if (chinese) FALLBACK_CN else FALLBACK_EN)
            .filter { norm(it) != norm(correct) && distractors.none { d -> norm(d) == norm(it) } }
            .shuffled()
        distractors = (distractors + fallback).take(3)
    }
    return (distractors + correct).shuffled() to usedAi
}

@Composable
fun QuizScreen(
    vm: AppViewModel,
    mode: QuizMode,
    onExit: () -> Unit,
    overrideWords: List<WordEntry>? = null,
    screenTitle: String? = null
) {
    val focusManager = LocalFocusManager.current

    var restartKey by remember { mutableIntStateOf(0) }
    var preparing by remember { mutableStateOf(true) }
    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var answers by remember { mutableStateOf<List<AnswerState>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var justAnswered by remember { mutableIntStateOf(-1) }
    val wrongWords = remember { mutableStateListOf<WordEntry>() }

    fun setAnswer(i: Int, a: AnswerState) {
        answers = answers.toMutableList().also { if (i < it.size) it[i] = a }
    }

    fun navigate(delta: Int) {
        focusManager.clearFocus()
        justAnswered = -1
        val target = index + delta
        if (target in questions.indices) index = target
        else if (delta > 0) index = questions.size // 最后一题的"查看结果"
    }

    LaunchedEffect(mode, restartKey) {
        preparing = true
        index = 0; score = 0; justAnswered = -1
        wrongWords.clear()
        val bank = (overrideWords ?: vm.words.value).shuffled()
        if (bank.isEmpty()) {
            questions = emptyList()
            answers = emptyList()
            preparing = false
            return@LaunchedEffect
        }
        val s = vm.settings.value
        val n = minOf(s.questionsPerRound, bank.size)
        val picked = bank.take(n)
        val all = vm.words.value
        // 四选一的干扰项走 AI，逐题并行请求（限流 4 并发），词库兜底
        val sem = Semaphore(4)
        val qs = coroutineScope {
            picked.map { w ->
                async {
                    sem.withPermit {
                        when (mode) {
                            QuizMode.EN_CN -> {
                                val pool = all.filter { it.id != w.id }.map { it.meaning }
                                val (opts, _) = buildOptions(w.meaning, pool, s, w, chinese = true, vm = vm)
                                Question(w, opts, opts.indexOfFirst { normalizeZh(it) == normalizeZh(w.meaning) })
                            }
                            QuizMode.CN_EN -> {
                                val pool = all.filter { it.id != w.id }.map { it.word }
                                val (opts, _) = buildOptions(w.word, pool, s, w, chinese = false, vm = vm)
                                Question(w, opts, opts.indexOfFirst { normalizeEn(it) == normalizeEn(w.word) })
                            }
                            QuizMode.DICTATION, QuizMode.SPELLING -> Question(w)
                        }
                    }
                }
            }.awaitAll()
        }
        answers = List(qs.size) { AnswerState() }
        questions = qs
        preparing = false
    }

    // 答对后 0.9s 自动跳转；答错停留展示正确答案，由用户点「下一题」继续。
    // 手动前进/后退会取消自动跳转。
    LaunchedEffect(justAnswered, index) {
        val a = answers.getOrNull(index)
        if (justAnswered >= 0 && justAnswered == index && a?.answered == true && a.correct) {
            delay(900L)
            if (justAnswered == index) {
                justAnswered = -1
                focusManager.clearFocus()
                index++
            }
        }
    }

    val finished = !preparing && questions.isNotEmpty() && index >= questions.size

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {
                focusManager.clearFocus()
                onExit()
            }) {
                Icon(Icons.Rounded.Close, contentDescription = "退出", modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(6.dp))
            Column {
                Text(screenTitle ?: mode.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                if (screenTitle != null) {
                    Text(mode.title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.weight(1f))
            if (questions.isNotEmpty()) {
                Text(
                    "${minOf(index + 1, questions.size)} / ${questions.size}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = {
                if (questions.isEmpty()) 0f
                else (index.toFloat() / questions.size).coerceIn(0f, 1f)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            strokeCap = StrokeCap.Round
        )
        Spacer(Modifier.height(16.dp))

        when {
            preparing -> Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(14.dp))
                    val usingAi = vm.settings.value.apiEnabled &&
                        (mode == QuizMode.EN_CN || mode == QuizMode.CN_EN)
                    Text(
                        if (usingAi) "AI 正在生成干扰项…" else "正在准备题目…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            questions.isEmpty() -> Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (overrideWords != null) "错题本里暂时没有可以重练的单词" else "词库为空，请先在首页导入单词",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = onExit) { Text("返回") }
                }
            }
            finished -> ResultContent(
                score = score,
                total = questions.size,
                wrongWords = wrongWords.toList(),
                onExit = onExit,
                onRestart = { restartKey++ },
                modifier = Modifier.weight(1f)
            )
            else -> {
                val q = questions[index]
                val a = answers.getOrNull(index) ?: AnswerState()
                val isLast = index == questions.size - 1

                if (mode == QuizMode.EN_CN || mode == QuizMode.CN_EN) {
                    ChoiceContent(
                        q = q,
                        chinese = mode == QuizMode.EN_CN,
                        answer = a,
                        modifier = Modifier.weight(1f)
                    ) { i ->
                        if (!a.answered) {
                            val ok = i == q.correctIndex
                            setAnswer(index, a.copy(selected = i, answered = true, correct = ok))
                            vm.recordResult(q.word.id, ok)
                            if (ok) {
                                score++
                                vm.removeWrong(q.word.id)
                            } else {
                                wrongWords.add(q.word)
                                vm.addWrong(q.word.id)
                            }
                            justAnswered = index
                        }
                    }
                } else {
                    TypedContent(
                        q = q,
                        mode = mode,
                        answer = a,
                        modifier = Modifier.weight(1f),
                        onTyped = { text -> setAnswer(index, a.copy(typed = text)) },
                        onSubmit = {
                            if (!a.answered && a.typed.isNotBlank()) {
                                val ok = if (mode == QuizMode.DICTATION) checkZhAnswer(a.typed, q.word.meaning)
                                else normalizeEn(a.typed) == normalizeEn(q.word.word)
                                setAnswer(index, a.copy(answered = true, correct = ok))
                                vm.recordResult(q.word.id, ok)
                                if (ok) {
                                    score++
                                    vm.removeWrong(q.word.id)
                                } else {
                                    wrongWords.add(q.word)
                                    vm.addWrong(q.word.id)
                                }
                                justAnswered = index
                            }
                        }
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { navigate(-1) },
                        enabled = index > 0,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("上一题") }
                    Button(
                        onClick = { navigate(1) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            when {
                                isLast -> "查看结果"
                                a.answered -> "下一题"
                                else -> "跳过"
                            }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun ChoiceContent(
    q: Question,
    chinese: Boolean,
    answer: AnswerState,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    val answered = answer.answered
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 34.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (chinese) "选出正确的中文释义" else "选出正确的英文单词",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (chinese) q.word.word else q.word.meaning,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 48.sp
                )
                if (q.word.pos.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            q.word.pos,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        q.options.forEachIndexed { i, opt ->
            val isCorrect = i == q.correctIndex
            val isPicked = i == answer.selected
            val borderColor = when {
                answered && isCorrect -> GREEN
                answered && isPicked -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            val containerColor = when {
                answered && isCorrect -> GREEN.copy(alpha = 0.08f)
                answered && isPicked -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }
            Card(
                onClick = { onSelect(i) },
                enabled = !answered,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                border = BorderStroke(1.5.dp, borderColor)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(28.dp)
                            .background(
                                if (answered && isCorrect) GREEN.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${'A' + i}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (answered && isCorrect) GREEN else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        opt,
                        fontSize = 16.sp,
                        fontWeight = if (answered && isCorrect) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            answered && isCorrect -> GREEN
                            answered && isPicked -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (answered && isCorrect) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = GREEN, modifier = Modifier.size(20.dp))
                    } else if (answered && isPicked) {
                        Icon(Icons.Rounded.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun TypedContent(
    q: Question,
    mode: QuizMode,
    answer: AnswerState,
    modifier: Modifier = Modifier,
    onTyped: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val isDictation = mode == QuizMode.DICTATION
    val answered = answer.answered
    val ok = answer.correct

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(q.word.id) {
        if (!answered) {
            delay(300)
            runCatching { focusRequester.requestFocus() }
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 34.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (isDictation) "请写出它的中文意思" else "请默写出对应的英文单词",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    if (isDictation) q.word.word else q.word.meaning,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 48.sp
                )
                if (q.word.pos.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            q.word.pos,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                if (!isDictation && q.word.word.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "提示：共 ${q.word.word.length} 个字母，首字母 ${q.word.word.first()}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = answer.typed,
            onValueChange = onTyped,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            enabled = !answered,
            minLines = 2,
            placeholder = { Text(if (isDictation) "输入中文意思…" else "输入英文单词…") },
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onSubmit() })
        )
        Spacer(Modifier.height(12.dp))

        if (!answered) {
            Button(
                onClick = onSubmit,
                enabled = answer.typed.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("提交", fontSize = 16.sp) }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (ok) GREEN.copy(alpha = 0.08f) else MaterialTheme.colorScheme.errorContainer,
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                    contentDescription = null,
                    tint = if (ok) GREEN else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        if (ok) "回答正确！" else "回答错误",
                        fontWeight = FontWeight.Bold,
                        color = if (ok) GREEN else MaterialTheme.colorScheme.error
                    )
                    if (!ok) {
                        val correctAnswer = if (isDictation) q.word.meaning else q.word.word
                        Text("正确答案：$correctAnswer", fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ResultContent(
    score: Int,
    total: Int,
    wrongWords: List<WordEntry>,
    onExit: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pct = if (total == 0) 0 else (score * 100 / total)
    val message = when {
        pct == 100 -> "太棒了，全部答对！"
        pct >= 80 -> "表现优秀！"
        pct >= 60 -> "不错，继续加油！"
        else -> "别灰心，再练一轮吧！"
    }
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(30.dp))
        Box(
            Modifier
                .size(132.dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$score", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                Text("/ $total 题", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(6.dp))
            Text(message, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "正确率 $pct%",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (wrongWords.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth()) {
                Text(
                    "错词回顾（${wrongWords.size}，已收进错题本）",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.height(10.dp))
            wrongWords.forEach { w ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(w.word, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        if (w.pos.isNotBlank()) {
                            Spacer(Modifier.width(6.dp))
                            Text(w.pos, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            w.meaning,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(26.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("返回首页") }
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp)
            ) { Text("再来一轮") }
        }
        Spacer(Modifier.height(20.dp))
    }
}
