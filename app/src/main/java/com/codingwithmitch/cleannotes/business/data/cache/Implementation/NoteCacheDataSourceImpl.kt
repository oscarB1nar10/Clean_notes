package com.codingwithmitch.cleannotes.business.data.cache.Implementation
import com.codingwithmitch.cleannotes.business.data.cache.Abstraction.NoteCacheDataSource
import com.codingwithmitch.cleannotes.business.domain.model.Note
import com.codingwithmitch.cleannotes.framework.datasource.cache.abstraction.NoteDaoService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteCacheDataSourceImpl
@Inject
constructor(
    private val noteDaoService: NoteDaoService
): NoteCacheDataSource {
    override suspend fun insertNote(note: Note)
            = noteDaoService.insertNote(note)

    override suspend fun deleteNote(primaryKey: String)
            = noteDaoService.deleteNote(primaryKey)

    override suspend fun deleteNotes(notes: List<Note>)
            = noteDaoService.deleteNotes(notes)

    override suspend fun updateNote(primaryKey: String, newTitle: String, newBody: String)
            = noteDaoService.updateNote(primaryKey, newTitle, newBody)

    override suspend fun searchNotes(query: String, filterAndOrder: String, page: Int): List<Note>{
        TODO("Check filterAndOrder and make query")
    }

    override suspend fun searchNoteById(primaryKey: String)
            = noteDaoService.searchNoteById(primaryKey)

    override suspend fun getNumNotes()
            = noteDaoService.getNumNotes()

    override suspend fun insertNotes(notes: List<Note>): LongArray{
        return noteDaoService.insertNotes(notes)
    }
}