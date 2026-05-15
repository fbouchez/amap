package com.amap.app.model

import java.io.InputStream
import java.util.Scanner

data class CsvParseResult(
    val people: List<Person>,
    val emptyHeaders: List<String>,
    val headerlessInfo: List<Pair<String, String>>
)

object CsvParser {

    fun parse(inputStream: InputStream): CsvParseResult {
        val scanner = Scanner(inputStream)
        return parseFromScanner(scanner)
    }

    fun parseRawTable(text: String): List<List<String>> {
        val scanner = Scanner(text)
        val rows = mutableListOf<List<String>>()
        var pending = StringBuilder()

        while (scanner.hasNextLine()) {
            val rawLine = scanner.nextLine()
            pending.append(rawLine).append('\n')

            val line = pending.toString().trimEnd('\n')
            if (!isQuotedComplete(line)) continue

            val trimmed = line.trim()
            pending = StringBuilder()

            if (trimmed.isBlank()) continue

            rows.add(parseCsvLine(trimmed).map { it.trim().replace('\n', ' ') })
        }
        scanner.close()
        return rows
    }

    fun parseFromString(text: String): CsvParseResult {
        val scanner = Scanner(text)
        return parseFromScanner(scanner)
    }

    private fun parseFromScanner(scanner: Scanner): CsvParseResult {
        val people = mutableListOf<Person>()
        val headersWithData = mutableSetOf<String>()
        val headerlessInfo = mutableListOf<Pair<String, String>>()
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
                headers = parts.map { it.trim().replace('\n', ' ') }
                continue
            }

            val name = parts.first().trim()
            if (name.isBlank()) continue

            val allItems = parts.drop(1)
                .mapIndexed { index, value ->
                    val header = headers.getOrElse(index + 1) { "" }
                    Item(header = header, value = value.trim().replace('\n', ' '))
                }

            for (item in allItems) {
                if (item.value.isNotBlank() && item.header.isNotBlank()) {
                    headersWithData.add(item.header)
                }
            }

            for (item in allItems) {
                if (item.header.isBlank() && item.value.isNotBlank()) {
                    headerlessInfo.add(name to item.value)
                }
            }

            val items = allItems.filter { it.value.isNotBlank() && it.header.isNotBlank() && !it.header.equals("Cotis", ignoreCase = true) }

            if (items.isNotEmpty()) {
                people.add(Person(name = name, items = items))
            }
        }
        scanner.close()

        val emptyHeaders = headers?.filter { it.isNotBlank() && it !in headersWithData } ?: emptyList()
        return CsvParseResult(people, emptyHeaders, headerlessInfo)
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
