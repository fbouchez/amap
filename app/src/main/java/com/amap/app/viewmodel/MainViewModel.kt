package com.amap.app.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.amap.app.model.CsvParser
import com.amap.app.model.Person
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class MainViewModel : ViewModel() {

    var people = mutableListOf<Person>()
        private set

    var showDone = false
        private set

    val visiblePeople: List<Person>
        get() = if (showDone) people else people.filter { !it.isDone }

    fun loadCsvFromUri(context: Context, uri: Uri) {
        val inputStream = context.contentResolver.openInputStream(uri)
        inputStream?.use { stream ->
            people = CsvParser.parse(stream).toMutableList()
        }
        saveState(context)
    }

    fun loadCsvFromFile(file: File): Boolean {
        if (!file.exists()) return false
        file.inputStream().use { stream ->
            people = CsvParser.parse(stream).toMutableList()
        }
        return people.isNotEmpty()
    }

    fun loadSampleData(context: Context) {
        val inputStream = context.assets.open("amap_sample.csv")
        inputStream.use { stream ->
            people = CsvParser.parse(stream).toMutableList()
        }
    }

    fun toggleShowDone() {
        showDone = !showDone
    }

    fun toggleItem(person: Person, index: Int) {
        if (index in person.checkedItems) {
            person.checkedItems.remove(index)
        } else {
            person.checkedItems.add(index)
        }
    }

    fun markDone(person: Person) {
        person.isDone = true
        person.checkedItems.clear()
    }

    fun resetAll() {
        people.forEach { person ->
            person.isDone = false
            person.checkedItems.clear()
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
        people = saved.map {
            Person(it.name, it.items, it.checkedItems.toMutableSet(), it.isDone)
        }.toMutableList()
        return true
    }

    private data class SerializablePerson(
        val name: String,
        val items: List<String>,
        val checkedItems: List<Int>,
        val isDone: Boolean
    )
}
