package com.amap.app

import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.amap.app.model.Person
import com.amap.app.ui.DetailScreen
import com.amap.app.ui.MainScreen
import com.amap.app.ui.theme.AmapTheme
import com.amap.app.viewmodel.MainViewModel
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var csvPicker: androidx.activity.result.ActivityResultLauncher<String>
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        csvPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                viewModel.loadCsvFromUri(this, it)
                rebuildContent()
            }
        }

        handleIntentCsv(intent)

        rebuildContent()
    }

    private fun rebuildContent() {
        setContent {
            AmapTheme {
                viewModel = viewModel()
                val hasData = remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    val hasSavedState = viewModel.restoreState(this@MainActivity)
                    if (!hasSavedState) {
                        val csvFile = File(filesDir, "current.csv")
                        val loaded = viewModel.loadCsvFromFile(csvFile)
                        Log.d("AMAP", "loadCsvFromFile(${csvFile.exists()}) = $loaded")
                        hasData.value = loaded
                    } else {
                        Log.d("AMAP", "restored saved state")
                        hasData.value = true
                    }
                }

                if (!hasData.value && viewModel.people.isEmpty()) {
                    ImportScreen(
                        onLoadSample = {
                            viewModel.loadSampleData(this@MainActivity)
                            hasData.value = true
                        },
                        onLoadCsv = { csvPicker.launch("text/*") }
                    )
                } else {
                    AppContent(viewModel, onReimport = { csvPicker.launch("text/*") })
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleIntentCsv(intent)
    }

    private fun handleIntentCsv(intent: android.content.Intent?) {
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

enum class Screen { Main, Detail }

@Composable
fun ImportScreen(
    onLoadSample: () -> Unit,
    onLoadCsv: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AMAP Distribution",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Chargez votre fichier CSV pour commencer",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onLoadCsv,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Charger un fichier CSV")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onLoadSample,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Utiliser un exemple")
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Format : première colonne = noms,\ncolonnes suivantes = articles à prendre",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun AppContent(viewModel: MainViewModel, onReimport: () -> Unit) {
    var currentScreen by remember { mutableStateOf(Screen.Main) }
    var selectedPerson by remember { mutableStateOf<Person?>(null) }

    when (currentScreen) {
        Screen.Main -> {
            MainScreen(
                viewModel = viewModel,
                onPersonClick = { person ->
                    selectedPerson = person
                    currentScreen = Screen.Detail
                },
                onReimport = onReimport
            )
        }
        Screen.Detail -> {
            selectedPerson?.let { person ->
                DetailScreen(
                    viewModel = viewModel,
                    person = person,
                    onBack = {
                        selectedPerson = null
                        currentScreen = Screen.Main
                    }
                )
            }
        }
    }
}
