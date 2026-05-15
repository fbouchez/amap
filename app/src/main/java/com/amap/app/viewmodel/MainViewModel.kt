package com.amap.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.amap.app.model.CsvParser
import com.amap.app.model.CsvParseResult
import com.amap.app.model.Item
import com.amap.app.model.Person
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

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
            applyParseResult(CsvParser.parse(stream))
        }
        saveState(context)
    }

    fun loadCsvFromFile(file: File): Boolean {
        if (!file.exists()) return false
        file.inputStream().use { stream ->
            applyParseResult(CsvParser.parse(stream))
        }
        return people.isNotEmpty()
    }

    fun loadSampleData(context: Context) {
        val inputStream = context.assets.open("amap_sample.csv")
        inputStream.use { stream ->
            applyParseResult(CsvParser.parse(stream))
        }
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
