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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jr.englishword.data.QuizMode
import com.jr.englishword.data.WordEntry
import com.jr.englishword.ui.AppViewModel
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
    var quizOverride by remember { mutableStateOf<List<WordEntry>?>(null) }
    var quizTitle by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg ->
        scope.launch { snackbarHostState.showSnackbar(msg) }
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
                            quizOverride = words
                            quizTitle = "错题重练"
                            screen = "quiz"
                        }
                    )
                    "quiz" -> QuizScreen(
                        vm = vm,
                        mode = QuizMode.valueOf(quizMode),
                        onExit = { screen = "home" },
                        overrideWords = quizOverride,
                        screenTitle = quizTitle
                    )
                    else -> HomeScreen(
                        vm = vm,
                        onStartQuiz = { m ->
                            quizMode = m.name
                            quizOverride = null
                            quizTitle = null
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
