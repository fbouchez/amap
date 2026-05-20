package com.amap.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import com.amap.app.model.Person
import com.amap.app.ui.QrCodeDisplayDialog
import com.amap.app.ui.QrSyncDialog
import com.amap.app.ui.generateQrCodeBitmap
import com.amap.app.viewmodel.MainViewModel
import com.amap.app.viewmodel.QrParseResult
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onPersonClick: (Person) -> Unit,
    onReimport: () -> Unit,
    onGoHome: () -> Unit,
    onViewDistribFile: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var quickMode by remember { mutableStateOf(false) }
    var showQrSyncDialog by remember { mutableStateOf(false) }
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var showHashMismatchDialog by remember { mutableStateOf(false) }
    var qrCodeBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var pendingMatrix by remember { mutableStateOf<List<String>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { content ->
            when (val parseResult = viewModel.parseQrData(content)) {
                is QrParseResult.Ok -> {
                    viewModel.applyMerge(parseResult.matrix)
                    scope.launch {
                        val sbResult = snackbarHostState.showSnackbar(
                            message = "Fusion réussie",
                            actionLabel = "Annuler"
                        )
                        if (sbResult == SnackbarResult.ActionPerformed) {
                            viewModel.rollbackMerge()
                        }
                    }
                }
                is QrParseResult.HashMismatch -> {
                    pendingMatrix = parseResult.matrix
                    showHashMismatchDialog = true
                }
                is QrParseResult.Error -> {
                    scope.launch {
                        snackbarHostState.showSnackbar(parseResult.message)
                    }
                }
            }
        }
    }

    LaunchedEffect(viewModel.downloadError) {
        viewModel.downloadError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearDownloadError()
        }
    }
    val visiblePeople by remember { derivedStateOf { viewModel.visiblePeople } }
    val enabledHeaders = viewModel.enabledHeaders
    val allHeaders by remember { derivedStateOf { viewModel.allHeaders } }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Tout réinitialiser ?") },
            text = { Text("Les coches et validations en cours seront perdues.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.resetAll()
                    showResetDialog = false
                }) { Text("Réinitialiser") }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) { Text("Annuler") }
            }
        )
    }

    if (showFilterDialog) {
        FilterDialog(
            allHeaders = allHeaders,
            enabledHeaders = enabledHeaders,
            onToggle = { viewModel.toggleColumn(it) },
            onDismiss = { showFilterDialog = false }
        )
    }

    if (showHelpDialog) {
        HelpDialog(onDismiss = { showHelpDialog = false })
    }

    if (showInfoDialog) {
        InfoDialog(
            emptyHeaders = viewModel.emptyHeaders,
            headerlessInfo = viewModel.headerlessInfo,
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showQrSyncDialog) {
        QrSyncDialog(
            onGenerate = {
                val qrData = viewModel.generateQrData()
                qrCodeBitmap = generateQrCodeBitmap(qrData)
                showQrCodeDialog = true
            },
            onScan = {
                scanLauncher.launch(ScanOptions().apply {
                    setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                    setPrompt("Scannez le QR de l'autre téléphone")
                    setBeepEnabled(false)
                    setOrientationLocked(false)
                })
            },
            onDismiss = { showQrSyncDialog = false }
        )
    }

    if (showQrCodeDialog && qrCodeBitmap != null) {
        QrCodeDisplayDialog(
            qrBitmap = qrCodeBitmap!!,
            onDismiss = { showQrCodeDialog = false }
        )
    }

    if (showHashMismatchDialog) {
        AlertDialog(
            onDismissRequest = { showHashMismatchDialog = false },
            title = { Text("CSV différent") },
            text = { Text("Les fichiers CSV des deux téléphones sont différents. Fusionner quand même ?") },
            confirmButton = {
                TextButton(onClick = {
                    pendingMatrix?.let { viewModel.applyMerge(it) }
                    showHashMismatchDialog = false
                    pendingMatrix = null
                    scope.launch {
                        val sbResult = snackbarHostState.showSnackbar(
                            message = "Fusion effectuée (CSV différents)",
                            actionLabel = "Annuler"
                        )
                        if (sbResult == SnackbarResult.ActionPerformed) {
                            viewModel.rollbackMerge()
                        }
                    }
                }) { Text("Fusionner") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showHashMismatchDialog = false
                    pendingMatrix = null
                }) { Text("Annuler") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 4.dp)) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Charger un fichier CSV") },
                                onClick = {
                                    showMenu = false
                                    onReimport()
                                },
                                leadingIcon = { Icon(Icons.Default.FileOpen, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Voir le fichier de distribution") },
                                onClick = {
                                    showMenu = false
                                    onViewDistribFile()
                                },
                                leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row {
                                        Text(if (viewModel.isDownloading) "Téléchargement…" else "Télécharger depuis Google Sheets")
                                        if (viewModel.isDownloading) {
                                            Spacer(Modifier.width(8.dp))
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        }
                                    }
                                },
                                onClick = {
                                    if (!viewModel.isDownloading) {
                                        showMenu = false
                                        viewModel.downloadFromGoogleSheets(context)
                                    }
                                },
                                enabled = !viewModel.isDownloading,
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Tout réinitialiser") },
                                onClick = {
                                    showMenu = false
                                    showResetDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Revenir à l'écran d'accueil") },
                                onClick = {
                                    showMenu = false
                                    onGoHome()
                                },
                                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) }
                            )
                        }
                    }
                },
                title = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filtrer les colonnes", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                        IconButton(onClick = { viewModel.toggleShowDone() }) {
                            Icon(
                                if (viewModel.showDone) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (viewModel.showDone) "Cacher passés" else "Afficher passés",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(onClick = { quickMode = !quickMode }) {
                            Icon(
                                if (quickMode) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = if (quickMode) "Quitter le mode validation rapide" else "Mode validation rapide",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(onClick = { showQrSyncDialog = true }) {
                            Icon(Icons.Default.QrCode, contentDescription = "Synchronisation", tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                },
                actions = {
                    Row(modifier = Modifier.padding(end = 4.dp)) {
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Informations")
                        }
                        IconButton(onClick = { showHelpDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Aide")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(visiblePeople, key = { it.name }) { person ->
                PersonRow(
                    person = person,
                    onClick = { onPersonClick(person) },
                    quickMode = quickMode,
                    onQuickToggle = {
                        if (person.isDone) viewModel.unmarkDone(person)
                        else viewModel.markDone(person)
                    },
                    enabledHeaders = enabledHeaders
                )
            }
        }
    }
}

@Composable
private fun FilterDialog(
    allHeaders: Set<String>,
    enabledHeaders: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filtrer les colonnes") },
        text = {
            Column {
                allHeaders.sorted().forEach { header ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(header) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (header in enabledHeaders)
                                Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(header, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
private fun InfoDialog(
    emptyHeaders: List<String>,
    headerlessInfo: List<Pair<String, String>>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Informations") },
        text = {
            Column {
                if (emptyHeaders.isEmpty() && headerlessInfo.isEmpty()) {
                    Text("Aucune information supplémentaire.", style = MaterialTheme.typography.bodyMedium)
                }
                emptyHeaders.forEach { header ->
                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(
                            text = header,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (emptyHeaders.isNotEmpty() && headerlessInfo.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                headerlessInfo.forEach { (_, value) ->
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aide") },
        text = {
            Column {
                HelpItem(Icons.Default.MoreVert, "Menu — charger un CSV, télécharger, réinitialiser, revenir à l'accueil")
                HelpItem(Icons.Default.FilterList, "Filtrer les colonnes — afficher/masquer des articles")
                HelpItem(Icons.Default.Visibility, "Afficher ou cacher les personnes déjà validées")
                HelpItem(Icons.Default.CheckBoxOutlineBlank, "Mode validation rapide — valider les personnes directement depuis la liste")
                HelpItem(Icons.Default.QrCode, "Synchronisation — générer/scanner un QR pour fusionner les coches")
                HelpItem(Icons.Default.Info, "Informations générales (code entrée, téléphones...)")
                HelpItem(Icons.AutoMirrored.Filled.HelpOutline, "Aide — cette fenêtre")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
private fun HelpItem(icon: ImageVector, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun PersonRow(
    person: Person,
    onClick: () -> Unit,
    quickMode: Boolean = false,
    onQuickToggle: () -> Unit = {},
    enabledHeaders: Set<String> = emptySet()
) {
    val totalVisible = person.items.count { it.header in enabledHeaders }
    val checkedVisible = person.checkedItems.count { person.items[it].header in enabledHeaders }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (person.isDone)
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (quickMode) {
                Icon(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable(onClick = onQuickToggle),
                    imageVector = if (person.isDone) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (person.isDone)
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else
                        MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = person.name,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textDecoration = if (person.isDone) TextDecoration.LineThrough else TextDecoration.None,
                color = if (person.isDone)
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else
                    MaterialTheme.colorScheme.onSurface
            )
            if (person.isDone) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            } else if (checkedVisible > 0) {
                Text(
                    text = "$checkedVisible/$totalVisible",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
