package com.gatheredthoughts.voicenotes.data

object NoteCategories {
    const val TASK = "Task"
    const val IDEA = "Idea"
    const val JOURNAL = "Journal"
    const val REMINDER = "Reminder"

    val ALL = listOf(TASK, IDEA, JOURNAL, REMINDER)

    fun isValid(category: String): Boolean = category in ALL
}
