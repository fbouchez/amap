package com.amap.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.amap.app.model.CsvParser
import com.amap.app.model.Item
import com.amap.app.model.Person
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class MainViewModel : ViewModel() {

    val people = mutableStateListOf<Person>()

    var showDone by mutableStateOf(false)
        private set

    val visiblePeople: List<Person>
        get() = if (showDone) people else people.filter { !it.isDone }

    fun loadCsvFromUri(context: Context, uri: Uri) {
        val inputStream = context.contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            people.clear()
            people.addAll(CsvParser.parse(stream))
        }
        saveState(context)
    }

    fun loadCsvFromFile(file: File): Boolean {
        if (!file.exists()) return false
        file.inputStream().use { stream ->
            people.clear()
            people.addAll(CsvParser.parse(stream))
        }
        return people.isNotEmpty()
    }

    fun loadSampleData(context: Context) {
        val inputStream = context.assets.open("amap_sample.csv")
        inputStream.use { stream ->
            people.clear()
            people.addAll(CsvParser.parse(stream))
        }
    }

    fun toggleShowDone() {
        showDone = !showDone
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
        people[idx] = person.copy(isDone = true, checkedItems = emptySet())
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
        context.getSharedPreferences("amap", Context.MODE_PRIVATE)
            .edit()
            .putString("state", json)
            .apply()
    }

    fun restoreState(context: Context): Boolean {
        val json = context.getSharedPreferences("amap", Context.MODE_PRIVATE)
            .getString("state", null) ?: return false
        val gson = Gson()
        val type = object : TypeToken<List<SerializablePerson>>() {}.type
        val saved: List<SerializablePerson> = gson.fromJson(json, type)
        people.clear()
        people.addAll(saved.map {
            Person(it.name, it.items.map { i -> Item(i.header, i.value) }, it.checkedItems.toSet(), it.isDone)
        })
        return true
    }

    private data class SerializablePerson(
        val name: String,
        val items: List<Item>,
        val checkedItems: List<Int>,
        val isDone: Boolean
    )
}
