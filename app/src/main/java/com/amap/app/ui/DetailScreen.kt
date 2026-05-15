package com.amap.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.automirrored.filled.Undo
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
fun DetailScreen(
    viewModel: MainViewModel,
    person: Person,
    onBack: () -> Unit
) {
    var showConfirm by remember { mutableStateOf(false) }
    val enabledHeaders = viewModel.enabledHeaders
    val visibleItems = remember(person, enabledHeaders) {
        person.items.withIndex().filter { (_, item) -> item.header in enabledHeaders }
    }
    val visibleCount = visibleItems.size
    val effectiveChecked = if (person.isDone) person.items.indices.toSet() else person.checkedItems
    val checkedVisible = visibleItems.count { (i, _) -> i in effectiveChecked }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("Valider ${person.name} ?") },
            text = {
                val remaining = visibleCount - checkedVisible
                Text("$remaining article(s) non cochés seront marqués comme non pris.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.markDone(person)
                    showConfirm = false
                    onBack()
                }) { Text("Valider quand même") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Retour") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = person.name,
                        textDecoration = if (person.isDone) TextDecoration.LineThrough else TextDecoration.None
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!person.isDone && checkedVisible == visibleCount) {
                            viewModel.markDone(person)
                        }
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp
            ) {
                if (person.isDone) {
                    OutlinedButton(
                        onClick = {
                            viewModel.unmarkDone(person)
                            onBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Annuler la validation")
                    }
                } else {
                    Button(
                        onClick = {
                            if (checkedVisible == visibleCount) {
                                viewModel.markDone(person)
                                onBack()
                            } else {
                                showConfirm = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (checkedVisible == visibleCount)
                                "Valider — tout pris !"
                            else
                                "Valider ($checkedVisible/$visibleCount)"
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "À prendre :",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            visibleItems.forEach { (index, item) ->
                val checked = index in effectiveChecked
                Card(
                    onClick = {
                        if (!person.isDone) {
                            viewModel.toggleItem(person, index)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (checked)
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.displayLabel,
                            style = MaterialTheme.typography.bodyLarge,
                            textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
                            color = if (checked)
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (checked)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}
