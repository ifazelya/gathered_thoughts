package com.gatheredthoughts.voicenotes.data

import kotlinx.coroutines.flow.Flow

class NotesRepository(private val noteDao: NoteDao) {

    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun getAllNotesOnce(): List<NoteEntity> = noteDao.getAllNotesOnce()

    suspend fun getNoteById(id: Int): NoteEntity? = noteDao.getNoteById(id)

    suspend fun insertNote(
        transcript: String,
        title: String,
        category: String
    ): Long {
        val note = NoteEntity(
            transcript = transcript,
            title = title,
            category = category,
            createdAt = System.currentTimeMillis()
        )
        return noteDao.insert(note)
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.update(note)
    }

    suspend fun deleteNote(note: NoteEntity) {
        noteDao.delete(note)
    }
}
