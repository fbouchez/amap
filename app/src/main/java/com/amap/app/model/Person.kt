package com.amap.app.model

data class Item(val header: String, val value: String) {
    val displayLabel: String get() = if (header.isBlank()) value else "$header: $value"
}

data class Person(
    val name: String,
    val items: List<Item>,
    val checkedItems: Set<Int> = emptySet(),
    val isDone: Boolean = false
)
