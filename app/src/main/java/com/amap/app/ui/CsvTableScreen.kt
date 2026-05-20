package com.amap.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.amap.app.model.CsvParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CsvTableScreen(rawCsvContent: String, onBack: () -> Unit) {
    val rows = remember(rawCsvContent) {
        if (rawCsvContent.isBlank()) emptyList()
        else CsvParser.parseRawTable(rawCsvContent)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fichier de distribution") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (rows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Aucune donnée disponible.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            val colCount = rows.maxOfOrNull { it.size } ?: 0
            val colWidths = remember(rows) {
                List(colCount) { c ->
                    val maxLen = rows.maxOf { r -> r.getOrElse(c) { "" }.length }
                    (maxLen * 8).coerceIn(60, 200)
                }
            }
            val hScrollState = rememberScrollState()
            val rowBg = Color(0xFFF5F5F5)
            val paddingMod = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                rows.forEachIndexed { i, row ->
                    Row(
                        modifier = Modifier
                            .heightIn(min = 28.dp)
                            .background(if (i > 0 && i % 2 == 0) rowBg else Color.Transparent)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(colWidths[0].dp)
                                .then(paddingMod),
                            contentAlignment = if (i == 0) Alignment.Center else Alignment.CenterStart
                        ) {
                            Text(
                                text = row.getOrElse(0) { "" },
                                style = if (i == 0) MaterialTheme.typography.titleSmall
                                        else MaterialTheme.typography.bodySmall,
                                color = if (i == 0) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(modifier = Modifier.horizontalScroll(hScrollState)) {
                            for (c in 1 until colCount) {
                                Box(
                                    modifier = Modifier
                                        .width(colWidths[c].dp)
                                        .then(paddingMod),
                                    contentAlignment = if (i == 0) Alignment.Center else Alignment.CenterStart
                                ) {
                                    Text(
                                        text = row.getOrElse(c) { "" },
                                        style = if (i == 0) MaterialTheme.typography.titleSmall
                                                else MaterialTheme.typography.bodySmall,
                                        color = if (i == 0) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    if (i == 0) HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}
