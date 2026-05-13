package com.amap.app.model

data class Person(
    val name: String,
    val items: List<String>,
    val checkedItems: MutableSet<Int> = mutableSetOf(),
    var isDone: Boolean = false
)
