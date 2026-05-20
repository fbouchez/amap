package com.amap.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.app.ui.CsvTableScreen
import com.amap.app.ui.DetailScreen
import com.amap.app.ui.HomeScreen
import com.amap.app.ui.MainScreen
import com.amap.app.ui.theme.AmapTheme
import com.amap.app.viewmodel.MainViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var csvPicker: ActivityResultLauncher<String>
    private lateinit var viewModel: MainViewModel

    private val currentScreen = mutableStateOf(Screen.Home)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        csvPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                viewModel.loadCsvFromUri(this, it)
                currentScreen.value = Screen.Main
            }
        }

        handleIntentCsv(intent)

        setContent {
            AmapTheme {
                viewModel = viewModel()
                AppContent(
                    viewModel = viewModel,
                    screen = currentScreen.value,
                    onNavigate = { currentScreen.value = it },
                    onPickCsv = { csvPicker.launch("text/*") }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntentCsv(intent)
        currentScreen.value = Screen.Home
    }

    private fun handleIntentCsv(intent: Intent?) {
        val b64 = intent?.getStringExtra("csv_b64") ?: return
        try {
            val decoded = Base64.decode(b64, Base64.DEFAULT)
            val content = String(decoded)
            File(filesDir, "current.csv").writeText(content)
            getSharedPreferences("amap", MODE_PRIVATE).edit().clear().apply()
            Log.d("AMAP", "handleIntentCsv: wrote ${content.length} bytes, cleared saved state")
        } catch (e: Exception) {
            Log.e("AMAP", "handleIntentCsv failed", e)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::viewModel.isInitialized) {
            viewModel.saveState(this)
        }
    }
}

enum class Screen { Home, Main, Detail, CsvTable }

@Composable
fun AppContent(
    viewModel: MainViewModel,
    screen: Screen,
    onNavigate: (Screen) -> Unit,
    onPickCsv: () -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (viewModel.people.isEmpty()) {
            val hasSavedState = viewModel.restoreState(context)
            if (!hasSavedState) {
                val csvFile = File(context.filesDir, "current.csv")
                if (!viewModel.loadCsvFromFile(csvFile)) {
                    viewModel.loadExampleData(context)
                }
            }
        }
    }

    var selectedName by remember { mutableStateOf<String?>(null) }

    when (screen) {
        Screen.Home -> {
            HomeScreen(
                viewModel = viewModel,
                onPickCsv = onPickCsv,
                onStart = { onNavigate(Screen.Main) }
            )
        }
        Screen.Main -> {
            MainScreen(
                viewModel = viewModel,
                onPersonClick = { person ->
                    selectedName = person.name
                    onNavigate(Screen.Detail)
                },
                onReimport = onPickCsv,
                onGoHome = { onNavigate(Screen.Home) },
                onViewDistribFile = { onNavigate(Screen.CsvTable) }
            )
        }
        Screen.Detail -> {
            selectedName?.let { name ->
                DetailScreen(
                    viewModel = viewModel,
                    person = viewModel.people.find { it.name == name } ?: return@let,
                    onBack = {
                        selectedName = null
                        onNavigate(Screen.Main)
                    }
                )
            }
        }
        Screen.CsvTable -> {
            CsvTableScreen(
                rawCsvContent = viewModel.rawCsvContent,
                onBack = { onNavigate(Screen.Main) }
            )
        }
    }
}
