package com.codingwithmitch.cleannotes.business.interactors.common

import com.codingwithmitch.cleannotes.business.data.cache.Abstraction.NoteCacheDataSource
import com.codingwithmitch.cleannotes.business.data.cache.CacheErrors.CACHE_ERROR_UNKNOWN
import com.codingwithmitch.cleannotes.business.data.cache.FORCE_DELETE_NOTE_EXCEPTION
import com.codingwithmitch.cleannotes.business.data.network.abstraction.NoteNetworkDataSource
import com.codingwithmitch.cleannotes.business.domain.model.Note
import com.codingwithmitch.cleannotes.business.domain.state.DataState
import com.codingwithmitch.cleannotes.business.interactors.common.DeleteNote.Companion.DELETE_NOTE_SUCCESS
import com.codingwithmitch.cleannotes.di.DependencyContainer
import com.codingwithmitch.cleannotes.framework.Presentation.notelist.state.NoteListStateEvent
import com.codingwithmitch.cleannotes.framework.Presentation.notelist.state.NoteListViewState
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.util.*

/*
Test cases:
1. deleteNote_success_confirmNetworkUpdated()
    a) delete a note
    b) check for success message from flow emission
    c) confirm note was deleted from "notes" node in network
    d) confirm note was added to "deletes" node in network
2. deleteNote_fail_confirmNetworkUnchanged()
    a) attempt to delete a note, fail since does not exist
    b) check for failure message from flow emission
    c) confirm network was not changed
3. throwException_checkGenericError_confirmNetworkUnchanged()
    a) attempt to delete a note, force an exception to throw
    b) check for failure message from flow emission
    c) confirm network was not changed
 */

@InternalCoroutinesApi
class DeleteNoteTest {

    // system in test
    private val deleteNote: DeleteNote<NoteListViewState>

    // dependencies
    private val dependencyContainer: DependencyContainer
    private val noteCacheDataSource: NoteCacheDataSource
    private val noteNetworkDataSource: NoteNetworkDataSource

    init {
        dependencyContainer = DependencyContainer()
        dependencyContainer.build()
        noteCacheDataSource = dependencyContainer.noteCacheDataSource
        noteNetworkDataSource = dependencyContainer.noteNetworkDataSource
        deleteNote = DeleteNote(
            noteCacheDataSource = noteCacheDataSource,
            noteNetworkDataSource = noteNetworkDataSource
        )
    }

    @Test
    fun deleteNote_success_confirmNetworkUpdated() = runBlocking {
        val noteToDelete = noteCacheDataSource.searchNotes(
            query = "",
            filterAndOrder = "",
            page = 1
        )[0]

        deleteNote.deleteNote(
            noteToDelete,
            NoteListStateEvent.DeleteNoteEvent(noteToDelete)
        ).collect(object : FlowCollector<DataState<NoteListViewState>?>{
            override suspend fun emit(value: DataState<NoteListViewState>?) {
               assertEquals(
                   value?.stateMessage?.response?.message,
                   DELETE_NOTE_SUCCESS
               )
            }
        })

        // Confirm was deleted from 'notes' node
        val wasNoteDeleted = !noteNetworkDataSource.getAllNotes()
            .contains(noteToDelete)
        assertTrue{wasNoteDeleted}

        // Confirm was inserted into 'deletes' note
        val wasDeletedNoteInserted = noteNetworkDataSource.getDeletedNotes()
            .contains(noteToDelete)
        assertTrue{wasDeletedNoteInserted}

    }

    @Test
    fun deleteNote_fail_confirmNetworkUnchanged() = runBlocking {
        val noteToDelete = Note(
            id = UUID.randomUUID().toString(),
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString(),
            created_at = UUID.randomUUID().toString(),
            updated_at = UUID.randomUUID().toString()
        )

        deleteNote.deleteNote(
            noteToDelete,
            NoteListStateEvent.DeleteNoteEvent(noteToDelete)
        ).collect(object : FlowCollector<DataState<NoteListViewState>?>{
            override suspend fun emit(value: DataState<NoteListViewState>?) {
                assertEquals(
                    value?.stateMessage?.response?.message,
                    DeleteNote.DELETE_NOTE_FAILURE
                )
            }
        })

        // Confirm was not deleted from 'notes' node
        val notesInNetwork = noteNetworkDataSource.getAllNotes()
        val notesInCache = noteCacheDataSource.getNumNotes()
        assertTrue{notesInNetwork.size == notesInCache}

        // Confirm was not inserted into 'deletes' note
        val wasDeletedNoteInserted = !noteNetworkDataSource.getDeletedNotes()
            .contains(noteToDelete)
        assertTrue{wasDeletedNoteInserted}
    }

    @Test
    fun throwException_checkGenericError_confirmNetworkUnchanged() = runBlocking {
        val noteToDelete = Note(
            id = FORCE_DELETE_NOTE_EXCEPTION,
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString(),
            created_at = UUID.randomUUID().toString(),
            updated_at = UUID.randomUUID().toString()
        )

        deleteNote.deleteNote(
            noteToDelete,
            NoteListStateEvent.DeleteNoteEvent(noteToDelete)
        ).collect(object : FlowCollector<DataState<NoteListViewState>?>{
            override suspend fun emit(value: DataState<NoteListViewState>?) {
                assert(
                    value?.stateMessage?.response?.message
                        ?.contains(CACHE_ERROR_UNKNOWN)?:false
                )
            }
        })

        // Confirm was not deleted from 'notes' node
        val notesInNetwork = noteNetworkDataSource.getAllNotes()
        val notesInCache = noteCacheDataSource.getNumNotes()
        assertTrue{notesInNetwork.size == notesInCache}

        // Confirm was not inserted into 'deletes' note
        val wasDeletedNoteInserted = !noteNetworkDataSource.getDeletedNotes()
            .contains(noteToDelete)
        assertTrue{wasDeletedNoteInserted}
    }
}