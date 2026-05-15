package com.amap.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.amap.app.model.Person
import com.amap.app.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onPersonClick: (Person) -> Unit,
    onReimport: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showCsvTableDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var quickMode by remember { mutableStateOf(false) }
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

    if (showCsvTableDialog) {
        CsvTableDialog(
            rawCsvContent = viewModel.rawCsvContent,
            onDismiss = { showCsvTableDialog = false }
        )
    }

    Scaffold(
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
                                    showCsvTableDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Tout réinitialiser") },
                                onClick = {
                                    showMenu = false
                                    showResetDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
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
                        IconButton(onClick = { quickMode = !quickMode }) {
                            Icon(
                                if (quickMode) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = if (quickMode) "Quitter le mode validation rapide" else "Mode validation rapide",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
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
private fun CsvTableDialog(rawCsvContent: String, onDismiss: () -> Unit) {
    val rows = remember(rawCsvContent) {
        if (rawCsvContent.isBlank()) emptyList()
        else com.amap.app.model.CsvParser.parseRawTable(rawCsvContent)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fichier de distribution") },
        text = {
            if (rows.isEmpty()) {
                Text("Aucune donnée disponible.", style = MaterialTheme.typography.bodyMedium)
            } else {
                val colCount = rows.maxOfOrNull { it.size } ?: 0
                val colWidths = remember(rows) {
                    List(colCount) { c ->
                        val maxLen = rows.maxOf { r -> r.getOrElse(c) { "" }.length }
                        (maxLen * 8).coerceIn(60, 200)
                    }
                }
                Box(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .verticalScroll(rememberScrollState())
                ) {
                    Column {
                        rows.forEachIndexed { i, row ->
                            Row(
                                modifier = Modifier.padding(vertical = 1.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                for (c in 0 until colCount) {
                                    Box(
                                        modifier = Modifier.width(colWidths[c].dp),
                                        contentAlignment = if (i == 0) Alignment.Center else Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = row.getOrElse(c) { "" },
                                            style = if (i == 0) MaterialTheme.typography.titleSmall
                                                    else MaterialTheme.typography.bodySmall,
                                            color = if (i == 0) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (i == 0) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
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
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aide") },
        text = {
            Column {
                HelpItem(Icons.Default.MoreVert, "Menu — charger un CSV, voir le fichier, réinitialiser")
                HelpItem(Icons.Default.CheckBoxOutlineBlank, "Mode validation rapide — valider les personnes directement depuis la liste")
                HelpItem(Icons.Default.FilterList, "Filtrer les colonnes — afficher/masquer des articles")
                HelpItem(Icons.Default.Visibility, "Afficher ou cacher les personnes déjà validées")
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
