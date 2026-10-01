package com.jr.englishword

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jr.englishword.data.QuizMode
import com.jr.englishword.ui.AppViewModel
import com.jr.englishword.ui.decodeWordListOrNull
import com.jr.englishword.ui.encodeWordList
import com.jr.englishword.ui.screens.HomeScreen
import com.jr.englishword.ui.screens.ImportScreen
import com.jr.englishword.ui.screens.ListScreen
import com.jr.englishword.ui.screens.QuizScreen
import com.jr.englishword.ui.screens.SettingsScreen
import com.jr.englishword.ui.screens.WrongBookScreen
import com.jr.englishword.ui.theme.EnglishWordsTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EnglishWordsTheme {
                val vm: AppViewModel = viewModel()
                AppRoot(vm)
            }
        }
    }
}

@Composable
fun AppRoot(vm: AppViewModel) {
    var screen by rememberSaveable { mutableStateOf("home") }
    var quizMode by rememberSaveable { mutableStateOf(QuizMode.EN_CN.name) }
    // 错题重练的题源以 JSON 串保存：旋转/进程重建后仍能保持"错题重练"，
    // 否则 overrideWords 归 null 会静默退化成全词库练习。
    var quizOverrideJson by rememberSaveable { mutableStateOf("") }
    val quizOverride = remember(quizOverrideJson) { decodeWordListOrNull(quizOverrideJson) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
    }

    // 落盘失败必须让用户看到，而不是让"已保存"的假象留在界面上
    val saveError by vm.saveError.collectAsState()
    LaunchedEffect(saveError) {
        saveError?.let {
            toast(it)
            vm.clearSaveError()
        }
    }

    BackHandler(enabled = screen != "home") { screen = "home" }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 14 })
                        .togetherWith(fadeOut(tween(120)))
                },
                label = "nav"
            ) { s ->
                when (s) {
                    "import" -> ImportScreen(vm = vm, toast = toast, goList = { screen = "list" }, onBack = { screen = "home" })
                    "list" -> ListScreen(vm = vm, toast = toast, onBack = { screen = "home" })
                    "settings" -> SettingsScreen(vm = vm, toast = toast, onBack = { screen = "home" })
                    "wrongbook" -> WrongBookScreen(
                        vm = vm,
                        toast = toast,
                        onBack = { screen = "home" },
                        onStartQuiz = { m, words ->
                            quizMode = m.name
                            quizOverrideJson = encodeWordList(words)
                            screen = "quiz"
                        }
                    )
                    "quiz" -> QuizScreen(
                        vm = vm,
                        mode = QuizMode.valueOf(quizMode),
                        onExit = { screen = "home" },
                        overrideWords = quizOverride,
                        screenTitle = if (quizOverride != null) "错题重练" else null
                    )
                    else -> HomeScreen(
                        vm = vm,
                        onStartQuiz = { m ->
                            quizMode = m.name
                            quizOverrideJson = ""
                            screen = "quiz"
                        },
                        goImport = { screen = "import" },
                        goList = { screen = "list" },
                        goWrongBook = { screen = "wrongbook" },
                        goSettings = { screen = "settings" },
                        toast = toast
                    )
                }
            }
        }
    }
}
