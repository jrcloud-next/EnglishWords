package com.jr.englishword.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jr.englishword.data.QuizMode
import com.jr.englishword.data.WordEntry
import com.jr.englishword.ui.AnswerState
import com.jr.englishword.ui.AppViewModel
import com.jr.englishword.ui.Question
import com.jr.englishword.ui.QuizSession
import com.jr.englishword.ui.QuizSessionSaver
import com.jr.englishword.ui.components.AnimatedCounter
import com.jr.englishword.ui.components.CircularRingProgress
import com.jr.englishword.ui.components.GradientBar
import com.jr.englishword.ui.components.InfoPill
import com.jr.englishword.ui.components.motionSpec
import com.jr.englishword.ui.components.pressableScale
import com.jr.englishword.ui.components.staggeredAppear
import com.jr.englishword.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull

/** 单题干扰项的等待上限；超时即退回词库/内置干扰项，避免整轮被最慢的一次请求拖住。 */
private const val OPTIONS_TIMEOUT_MS = 10_000L

/** 同时进行的干扰项请求数上限。 */
private const val AI_CONCURRENCY = 4

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

/**
 * 组装四选一选项：优先用已取得的 AI 干扰项，不足时依次从词库、内置列表补齐。
 * 网络请求由调用方在超时保护下完成，本函数不做 IO。
 */
private fun buildOptions(
    correct: String,
    pool: List<String>,
    ai: List<String>,
    chinese: Boolean
): List<String> {
    val norm: (String) -> String = if (chinese) ::normalizeZh else ::normalizeEn

    var distractors = ai.map { it.trim() }
        .filter { it.isNotEmpty() && norm(it) != norm(correct) }
        .distinctBy { norm(it) }
        .take(3)

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
    return (distractors + correct).shuffled()
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

    // 整轮会话整体可保存：旋转或进程重建后不丢进度，错题重练也不会退化成全词库练习
    var session by rememberSaveable(stateSaver = QuizSessionSaver) { mutableStateOf(QuizSession()) }
    // 仅用于触发"答对后 0.9s 自动跳转"，属于瞬时信号，无需保存
    var justAnswered by remember { mutableIntStateOf(-1) }

    val questions = session.questions
    val answers = session.answers
    val index = session.index
    val preparing = session.preparing

    // 种子变化才重新生成题目；seed 相同说明题目已就绪（含从 savedInstanceState 恢复的情况）
    val seed = remember(mode, session.restartKey, overrideWords) {
        buildString {
            append(mode.name).append('#').append(session.restartKey).append('#')
            if (overrideWords == null) append("ALL") else overrideWords.joinTo(this, ",") { it.id }
        }
    }

    /** 选项只写入一次，且不覆盖已作答的题目，避免选项与作答下标错位。 */
    fun setOptions(i: Int, wordId: String, options: List<String>, correctIndex: Int) {
        val current = session.questions.getOrNull(i) ?: return
        if (current.word.id != wordId) return // 上一轮的迟到结果，丢弃
        if (current.options.isNotEmpty()) return
        if (session.answers.getOrNull(i)?.answered == true) return
        session = session.copy(
            questions = session.questions.toMutableList()
                .also { it[i] = current.copy(options = options, correctIndex = correctIndex) }
        )
    }

    /** 提交当前题作答；以会话状态为准做守卫，重复触发不会重复计分。 */
    fun submitAnswer(correct: Boolean, selected: Int = -1) {
        val i = session.index
        val a = session.answers.getOrNull(i) ?: return
        if (a.answered) return
        val q = session.questions.getOrNull(i) ?: return
        session = session.copy(
            answers = session.answers.toMutableList().also {
                it[i] = a.copy(
                    selected = if (selected >= 0) selected else a.selected,
                    answered = true,
                    correct = correct
                )
            },
            score = if (correct) session.score + 1 else session.score,
            wrongWords = if (correct) session.wrongWords else session.wrongWords + q.word
        )
        vm.recordResult(q.word.id, correct)
        if (correct) vm.removeWrong(q.word.id) else vm.addWrong(q.word.id)
        justAnswered = i
    }

    fun navigate(delta: Int) {
        focusManager.clearFocus()
        justAnswered = -1
        val target = session.index + delta
        session = when {
            target in session.questions.indices -> session.copy(index = target)
            delta > 0 -> session.copy(index = session.questions.size)
            else -> session
        }
    }

    LaunchedEffect(seed) {
        if (session.seed == seed) return@LaunchedEffect

        val bank = (overrideWords ?: vm.words.value).shuffled()
        val s = vm.settings.value
        val picked = bank.take(minOf(s.questionsPerRound, bank.size))
        val isChoice = mode == QuizMode.EN_CN || mode == QuizMode.CN_EN

        // 题目一次性落位，首题立即可见；选项随后逐题补齐，不再等整轮 AI 请求
        session = QuizSession(
            seed = seed,
            restartKey = session.restartKey,
            questions = picked.map { Question(it) },
            answers = List(picked.size) { AnswerState() },
            preparing = false
        )
        if (picked.isEmpty() || !isChoice) return@LaunchedEffect

        val all = vm.words.value
        val chinese = mode == QuizMode.EN_CN
        val sem = Semaphore(AI_CONCURRENCY)
        picked.forEachIndexed { i, w ->
            launch {
                sem.withPermit {
                    val correct = if (chinese) w.meaning else w.word
                    val pool = all.filter { it.id != w.id }.map { if (chinese) it.meaning else it.word }
                    val ai = withTimeoutOrNull(OPTIONS_TIMEOUT_MS) {
                        vm.aiDistractors(w, chinese)
                    } ?: emptyList()
                    val opts = buildOptions(correct, pool, ai, chinese)
                    val norm: (String) -> String = if (chinese) ::normalizeZh else ::normalizeEn
                    setOptions(i, w.id, opts, opts.indexOfFirst { norm(it) == norm(correct) })
                }
            }
        }
    }

    // 答对后 0.9s 自动跳转；答错停留展示正确答案，由用户点「下一题」继续。
    // 手动前进/后退会取消自动跳转。
    LaunchedEffect(justAnswered, session.index) {
        val a = session.answers.getOrNull(session.index)
        if (justAnswered >= 0 && justAnswered == session.index && a?.answered == true && a.correct) {
            delay(900L)
            if (justAnswered == session.index) {
                justAnswered = -1
                focusManager.clearFocus()
                session = session.copy(index = session.index + 1)
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
                InfoPill(
                    text = "${minOf(index + 1, questions.size)} / ${questions.size}",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        GradientBar(
            progress = if (questions.isEmpty()) 0f else index.toFloat() / questions.size,
            stops = LocalAppColors.current.ringStops
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
                    Text(
                        "正在准备题目…",
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
                score = session.score,
                total = questions.size,
                wrongWords = session.wrongWords,
                onExit = onExit,
                onRestart = {
                    justAnswered = -1
                    session = session.copy(restartKey = session.restartKey + 1)
                },
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
                        if (!a.answered && q.correctIndex >= 0) {
                            submitAnswer(i == q.correctIndex, selected = i)
                        }
                    }
                } else {
                    TypedContent(
                        q = q,
                        mode = mode,
                        answer = a,
                        modifier = Modifier.weight(1f),
                        onTyped = { text ->
                            val i = session.index
                            val cur = session.answers.getOrNull(i)
                            if (cur != null && !cur.answered) {
                                session = session.copy(
                                    answers = session.answers.toMutableList()
                                        .also { it[i] = cur.copy(typed = text) }
                                )
                            }
                        },
                        onSubmit = {
                            val i = session.index
                            val cur = session.answers.getOrNull(i)
                            if (cur != null && !cur.answered && cur.typed.isNotBlank()) {
                                val ok = if (mode == QuizMode.DICTATION) {
                                    checkZhAnswer(cur.typed, q.word.meaning)
                                } else {
                                    normalizeEn(cur.typed) == normalizeEn(q.word.word)
                                }
                                submitAnswer(ok)
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
                            .heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) { Text("上一题") }
                    Button(
                        onClick = { navigate(1) },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = MaterialTheme.shapes.medium
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
    val appColors = LocalAppColors.current
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.5.dp, Brush.linearGradient(appColors.heroStops)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
        if (q.options.isEmpty()) {
            // 选项尚在生成：只占位，不伪造内容；仍可用「跳过」前进
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "正在生成选项…",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            q.options.forEachIndexed { i, opt ->
                val isCorrect = i == q.correctIndex
                val isPicked = i == answer.selected
                val borderColor by animateColorAsState(
                    targetValue = when {
                        answered && isCorrect -> appColors.success
                        answered && isPicked -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                    animationSpec = motionSpec(durationMillis = 220),
                    label = "optBorder"
                )
                val containerColor by animateColorAsState(
                    targetValue = when {
                        answered && isCorrect -> appColors.successContainer
                        answered && isPicked -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.surfaceContainerLowest
                    },
                    animationSpec = motionSpec(durationMillis = 220),
                    label = "optContainer"
                )
                val optionSource = remember { MutableInteractionSource() }
                Card(
                    onClick = { onSelect(i) },
                    enabled = !answered,
                    interactionSource = optionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .staggeredAppear(i, stepMillis = 40)
                        .pressableScale(optionSource),
                    shape = MaterialTheme.shapes.medium,
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
                                .size(30.dp)
                                .background(
                                    when {
                                        answered && isCorrect -> appColors.success.copy(alpha = 0.18f)
                                        answered && isPicked -> MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${'A' + i}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    answered && isCorrect -> appColors.success
                                    answered && isPicked -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            opt,
                            fontSize = 16.sp,
                            fontWeight = if (answered && isCorrect) FontWeight.Bold else FontWeight.Normal,
                            color = when {
                                answered && isCorrect -> appColors.success
                                answered && isPicked -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (answered && isCorrect) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = appColors.success, modifier = Modifier.size(20.dp))
                        } else if (answered && isPicked) {
                            Icon(Icons.Rounded.Cancel, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
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
    val appColors = LocalAppColors.current

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
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.5.dp, Brush.linearGradient(appColors.heroStops)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
            shape = MaterialTheme.shapes.medium,
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
                    .heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.medium
            ) { Text("提交", fontSize = 16.sp) }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        if (ok) appColors.successContainer else MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
                    contentDescription = null,
                    tint = if (ok) appColors.success else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        if (ok) "回答正确！" else "回答错误",
                        fontWeight = FontWeight.Bold,
                        color = if (ok) appColors.success else MaterialTheme.colorScheme.error
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
    val appColors = LocalAppColors.current
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
        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            border = BorderStroke(1.5.dp, Brush.linearGradient(appColors.heroStops)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 26.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // 满分时在环外补一层柔和光晕
                    if (pct == 100) {
                        Box(
                            Modifier
                                .size(196.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            appColors.ringStops.first().copy(alpha = 0.20f),
                                            Color.Transparent
                                        )
                                    ),
                                    CircleShape
                                )
                        )
                    }
                    CircularRingProgress(
                        progress = if (total == 0) 0f else score.toFloat() / total,
                        stops = appColors.ringStops,
                        size = 156.dp,
                        strokeWidth = 14.dp
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            AnimatedCounter(
                                value = score,
                                style = TextStyle(
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 44.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "/ $total 题",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(message, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "正确率 $pct%",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

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
                    shape = MaterialTheme.shapes.medium,
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
                    .heightIn(min = 50.dp),
                shape = MaterialTheme.shapes.medium
            ) { Text("返回首页") }
            Button(
                onClick = onRestart,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 50.dp),
                shape = MaterialTheme.shapes.medium
            ) { Text("再来一轮") }
        }
        Spacer(Modifier.height(20.dp))
    }
}
