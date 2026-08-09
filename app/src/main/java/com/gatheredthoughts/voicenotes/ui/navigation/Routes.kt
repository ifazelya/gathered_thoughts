package com.gatheredthoughts.voicenotes.ui.navigation

object Routes {
    const val RECORD = "record"
    const val LIST = "list"
    const val QUERY = "query"
    const val DETAIL = "detail/{noteId}"

    fun detail(noteId: Int) = "detail/$noteId"
}
