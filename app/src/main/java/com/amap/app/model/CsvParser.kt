package com.amap.app.model

import java.io.InputStream
import java.util.Scanner

object CsvParser {

    fun parse(inputStream: InputStream): List<Person> {
        val scanner = Scanner(inputStream)
        val people = mutableListOf<Person>()
        var pending = StringBuilder()
        var headers: List<String>? = null

        while (scanner.hasNextLine()) {
            val rawLine = scanner.nextLine()
            pending.append(rawLine).append('\n')

            val line = pending.toString().trimEnd('\n')
            if (!isQuotedComplete(line)) continue

            val trimmed = line.trim()
            pending = StringBuilder()

            if (trimmed.isBlank() || trimmed.startsWith("#") || trimmed.startsWith("//")) continue

            val parts = parseCsvLine(trimmed)
            if (parts.size < 2) continue

            if (headers == null) {
                headers = parts.map { it.trim() }
                continue
            }

            val name = parts.first().trim()
            if (name.isBlank()) continue

            val items = parts.drop(1)
                .mapIndexed { index, value ->
                    val header = headers.getOrElse(index + 1) { "" }.trim()
                    Item(header = header, value = value.trim())
                }
                .filter { it.value.isNotBlank() }

            if (items.isNotEmpty()) {
                people.add(Person(name = name, items = items))
            }
        }
        scanner.close()
        return people
    }

    private fun isQuotedComplete(line: String): Boolean {
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            when (line[i]) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
            }
            i++
        }
        return !inQuotes
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
