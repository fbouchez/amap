package com.amap.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amap.app.model.CsvParser
import com.amap.app.model.CsvParseResult
import com.amap.app.model.Item
import com.amap.app.model.Person
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class MainViewModel : ViewModel() {

    val people = mutableStateListOf<Person>()

    var showDone by mutableStateOf(false)
        private set

    var enabledHeaders by mutableStateOf<Set<String>>(emptySet())
        private set

    var emptyHeaders by mutableStateOf<List<String>>(emptyList())
        private set

    var headerlessInfo by mutableStateOf<List<Pair<String, String>>>(emptyList())
        private set

    var rawCsvContent by mutableStateOf("")
        private set

    var isDownloading by mutableStateOf(false)
        private set

    var downloadError by mutableStateOf<String?>(null)
        private set

    val allHeaders: Set<String>
        get() = people.flatMap { it.items.map { i -> i.header } }.toSet()

    val visiblePeople: List<Person>
        get() = if (showDone) people else people.filter { !it.isDone }

    private fun applyParseResult(result: CsvParseResult) {
        people.clear()
        people.addAll(result.people)
        emptyHeaders = result.emptyHeaders
        headerlessInfo = result.headerlessInfo
        initEnabledHeaders()
    }

    fun loadCsvFromUri(context: Context, uri: Uri) {
        val inputStream = context.contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            rawCsvContent = stream.bufferedReader().readText()
            applyParseResult(CsvParser.parseFromString(rawCsvContent))
        }
        saveState(context)
    }

    fun loadCsvFromFile(file: File): Boolean {
        if (!file.exists()) return false
        rawCsvContent = file.readText()
        applyParseResult(CsvParser.parseFromString(rawCsvContent))
        return people.isNotEmpty()
    }

    fun loadExampleData(context: Context) {
        val inputStream = context.assets.open("example.csv")
        rawCsvContent = inputStream.bufferedReader().readText()
        applyParseResult(CsvParser.parseFromString(rawCsvContent))
    }

    private val googleSheetsUrl = "https://docs.google.com/spreadsheets/d/1JJHBpxk37C8hAU1xSNGj9R4-GuwEdkXtAy22MuCjvTc/export?format=csv"

    fun downloadFromGoogleSheets(context: Context) {
        if (isDownloading) return
        isDownloading = true
        downloadError = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = URL(googleSheetsUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.instanceFollowRedirects = true
                val text = connection.inputStream.bufferedReader().readText()
                if (text.isBlank()) throw Exception("Fichier vide")
                withContext(Dispatchers.Main) {
                    rawCsvContent = text
                    applyParseResult(CsvParser.parseFromString(text))
                    saveState(context)
                    isDownloading = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    downloadError = "Échec du téléchargement : ${e.message ?: "erreur inconnue"}"
                    isDownloading = false
                }
            }
        }
    }

    fun clearDownloadError() {
        downloadError = null
    }

    fun toggleShowDone() {
        showDone = !showDone
    }

    fun toggleColumn(header: String) {
        enabledHeaders = if (header in enabledHeaders) enabledHeaders - header else enabledHeaders + header
    }

    private fun initEnabledHeaders() {
        enabledHeaders = allHeaders.filter { !it.equals("Cotis", ignoreCase = true) }.toSet()
    }

    fun toggleItem(person: Person, index: Int) {
        val idx = people.indexOf(person)
        if (idx < 0) return
        val updated = if (index in person.checkedItems)
            person.copy(checkedItems = person.checkedItems - index)
        else
            person.copy(checkedItems = person.checkedItems + index)
        people[idx] = updated
    }

    fun markDone(person: Person) {
        val idx = people.indexOf(person)
        if (idx < 0) return
        people[idx] = person.copy(isDone = true, checkedItems = person.items.indices.toSet())
    }

    fun unmarkDone(person: Person) {
        val idx = people.indexOf(person)
        if (idx < 0) return
        people[idx] = person.copy(isDone = false, checkedItems = emptySet())
    }

    fun resetAll() {
        for (i in people.indices) {
            people[i] = people[i].copy(isDone = false, checkedItems = emptySet())
        }
    }

    fun generateQrData(): String {
        val hash = computeCsvHash()
        val rows = people.map { person ->
            person.items.indices.joinToString("") { i ->
                if (i in person.checkedItems) "1" else "0"
            }
        }
        return "$hash|${rows.joinToString("|")}"
    }

    private var snapshot: List<Person>? = null

    fun parseQrData(data: String): QrParseResult {
        val parts = data.split("|")
        if (parts.size < 2) return QrParseResult.Error("Format de QR invalide")

        val hash = parts[0]
        val matrix = parts.drop(1)

        if (matrix.size != people.size) {
            return QrParseResult.Error("Nombre de personnes différent (${matrix.size} vs ${people.size})")
        }

        val expectedHash = computeCsvHash()
        if (hash != expectedHash) {
            return QrParseResult.HashMismatch(matrix, hash, expectedHash)
        }

        return QrParseResult.Ok(matrix)
    }

    fun applyMerge(matrix: List<String>) {
        saveSnapshot()
        for (i in people.indices) {
            val row = matrix[i]
            for (j in row.indices) {
                if (row[j] == '1' && j < people[i].items.size) {
                    if (j !in people[i].checkedItems) {
                        people[i] = people[i].copy(checkedItems = people[i].checkedItems + j)
                    }
                }
            }
            if (people[i].checkedItems.size >= people[i].items.size) {
                people[i] = people[i].copy(isDone = true)
            }
        }
    }

    fun rollbackMerge() {
        snapshot?.let { saved ->
            people.clear()
            people.addAll(saved)
            snapshot = null
        }
    }

    private fun saveSnapshot() {
        snapshot = people.toList()
    }

    private fun computeCsvHash(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(rawCsvContent.toByteArray())
        return hash.take(4).joinToString("") { "%02x".format(it) }
    }

    fun saveState(context: Context) {
        val gson = Gson()
        val json = gson.toJson(people.map {
            SerializablePerson(it.name, it.items, it.checkedItems.toList(), it.isDone)
        })
        val headerlessJson = gson.toJson(headerlessInfo.map { HeaderlessEntry(it.first, it.second) })
        val prefs = context.getSharedPreferences("amap", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("state", json)
            .putString("enabledHeaders", enabledHeaders.joinToString(","))
            .putString("emptyHeaders", emptyHeaders.joinToString(","))
            .putString("headerlessInfo", headerlessJson)
            .apply()
        context.openFileOutput("raw.csv", Context.MODE_PRIVATE).use {
            it.write(rawCsvContent.toByteArray())
        }
    }

    fun restoreState(context: Context): Boolean {
        val prefs = context.getSharedPreferences("amap", Context.MODE_PRIVATE)
        val json = prefs.getString("state", null) ?: return false
        val gson = Gson()
        val type = object : TypeToken<List<SerializablePerson>>() {}.type
        val saved: List<SerializablePerson> = gson.fromJson(json, type)
        people.clear()
        people.addAll(saved.map {
            Person(it.name, it.items.map { i -> Item(i.header, i.value) }, it.checkedItems.toSet(), it.isDone)
        })
        val savedHeaders = prefs.getString("enabledHeaders", null)
        enabledHeaders = if (savedHeaders != null) savedHeaders.split(",").filter { it.isNotEmpty() }.toSet()
            else allHeaders.filter { !it.equals("Cotis", ignoreCase = true) }.toSet()
        val savedEmptyHeaders = prefs.getString("emptyHeaders", null)
        emptyHeaders = if (savedEmptyHeaders != null) savedEmptyHeaders.split(",").filter { it.isNotEmpty() } else emptyList()
        val savedHeaderlessJson = prefs.getString("headerlessInfo", null)
        if (savedHeaderlessJson != null) {
            val entryType = object : TypeToken<List<HeaderlessEntry>>() {}.type
            val entries: List<HeaderlessEntry> = gson.fromJson(savedHeaderlessJson, entryType)
            headerlessInfo = entries.map { it.name to it.value }
        }
        val rawFile = File(context.filesDir, "raw.csv")
        if (rawFile.exists()) rawCsvContent = rawFile.readText()
        return true
    }

    private data class HeaderlessEntry(val name: String, val value: String)

    private data class SerializablePerson(
        val name: String,
        val items: List<Item>,
        val checkedItems: List<Int>,
        val isDone: Boolean
    )
}

sealed class QrParseResult {
    data class Ok(val matrix: List<String>) : QrParseResult()
    data class HashMismatch(val matrix: List<String>, val actualHash: String, val expectedHash: String) : QrParseResult()
    data class Error(val message: String) : QrParseResult()
}
