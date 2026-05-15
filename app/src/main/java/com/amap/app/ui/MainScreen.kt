package com.amap.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AMAP Distribution") },
                actions = {
                    IconButton(onClick = { quickMode = !quickMode }) {
                        Icon(
                            if (quickMode) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = if (quickMode) "Quitter le mode validation rapide" else "Mode validation rapide"
                        )
                    }
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrer les colonnes")
                    }
                    IconButton(onClick = onReimport) {
                        Icon(Icons.Default.FileOpen, contentDescription = "Charger un CSV")
                    }
                    IconButton(onClick = { viewModel.toggleShowDone() }) {
                        Icon(
                            if (viewModel.showDone) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (viewModel.showDone) "Cacher passés" else "Afficher passés"
                        )
                    }
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Tout réinitialiser")
                    }
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Aide")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
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
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aide") },
        text = {
            Column {
                HelpItem("[\u2611]", "Mode validation rapide — valider les personnes directement depuis la liste")
                HelpItem("[\u2630]", "Filtrer les colonnes — afficher/masquer des articles")
                HelpItem("[  \u00d7  ]", "Charger un fichier CSV")
                HelpItem("[\u25c9]", "Afficher ou cacher les personnes déjà validées")
                HelpItem("[\u21bb]", "Tout réinitialiser — effacer les coches et validations")
                HelpItem("[  ?  ]", "Aide — cette fenêtre")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
private fun HelpItem(icon: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(8.dp))
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
