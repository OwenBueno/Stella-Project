package com.stella.core.database

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: String?): List<String> =
        value?.split(SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()

    @TypeConverter
    fun toStringList(list: List<String>): String = list.joinToString(SEPARATOR)

    companion object {
        private const val SEPARATOR = "\u001F"
    }
}
