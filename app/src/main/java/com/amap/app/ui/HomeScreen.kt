package com.amap.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.amap.app.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onPickCsv: () -> Unit,
    onStart: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingDownload by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.clearDownloadError()
    }

    LaunchedEffect(viewModel.downloadError) {
        viewModel.downloadError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDownloadError()
        }
    }

    LaunchedEffect(pendingDownload, viewModel.isDownloading) {
        if (pendingDownload && !viewModel.isDownloading) {
            pendingDownload = false
            if (viewModel.downloadError == null && viewModel.people.isNotEmpty()) {
                onStart()
            }
        }
    }

    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                text = "Prêt pour la distribution ?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (viewModel.people.isEmpty()) {
                        val csvFile = File(context.filesDir, "current.csv")
                        if (!viewModel.loadCsvFromFile(csvFile)) {
                            viewModel.loadExampleData(context)
                        }
                    }
                    if (viewModel.people.isNotEmpty()) onStart()
                    else scope.launch {
                        snackbarHostState.showSnackbar("Aucun fichier disponible.")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isDownloading
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Démarrer avec le csv courant")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onPickCsv,
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isDownloading
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Charger un CSV")
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    pendingDownload = true
                    viewModel.downloadFromGoogleSheets(context)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewModel.isDownloading
            ) {
                if (viewModel.isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Téléchargement…")
                } else {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Télécharger le tableau de distribution")
                }
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
