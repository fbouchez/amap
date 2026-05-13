package com.amap.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileOpen
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
    val visiblePeople by remember { derivedStateOf { viewModel.visiblePeople } }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AMAP Distribution") },
                actions = {
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
                PersonRow(person = person, onClick = { onPersonClick(person) })
            }
        }
    }
}

@Composable
fun PersonRow(person: Person, onClick: () -> Unit) {
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
            Text(
                text = person.name,
                style = MaterialTheme.typography.titleMedium,
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
            } else {
                val checkedCount = person.checkedItems.size
                if (checkedCount > 0) {
                    Text(
                        text = "$checkedCount/${person.items.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
