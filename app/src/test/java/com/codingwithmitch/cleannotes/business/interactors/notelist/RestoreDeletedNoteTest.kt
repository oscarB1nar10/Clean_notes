package com.codingwithmitch.cleannotes.business.interactors.notelist

import com.codingwithmitch.cleannotes.business.data.cache.Abstraction.NoteCacheDataSource
import com.codingwithmitch.cleannotes.business.data.network.abstraction.NoteNetworkDataSource
import com.codingwithmitch.cleannotes.business.domain.model.Note
import com.codingwithmitch.cleannotes.business.domain.model.NoteFactory
import com.codingwithmitch.cleannotes.business.domain.state.DataState
import com.codingwithmitch.cleannotes.business.interactors.notelist.RestoreDeletedNote.Companion.RESTORE_NOTE_FAILED
import com.codingwithmitch.cleannotes.business.interactors.notelist.RestoreDeletedNote.Companion.RESTORE_NOTE_SUCCESS
import com.codingwithmitch.cleannotes.di.DependencyContainer
import com.codingwithmitch.cleannotes.framework.Presentation.notelist.state.NoteListStateEvent
import com.codingwithmitch.cleannotes.framework.Presentation.notelist.state.NoteListViewState
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.collections.ArrayList


/*
Test cases:
1. restoreNote_success_confirmCacheAndNetworkUpdated()
    a) create a new note and insert it into the "deleted" node of network
    b) restore that note
    c) Listen for success msg RESTORE_NOTE_SUCCESS from flow
    d) confirm note is in the cache
    e) confirm note is in the network "notes" node
    f) confirm note is not in the network "deletes" node
2. restoreNote_fail_confirmCacheAndNetworkUnchanged()
    a) create a new note and insert it into the "deleted" node of network
    b) restore that note (force a failure)
    c) Listen for success msg RESTORE_NOTE_FAILED from flow
    d) confirm note is not in the cache
    e) confirm note is not in the network "notes" node
    f) confirm note is in the network "deletes" node
3. throwException_checkGenericError_confirmNetworkAndCacheUnchanged()
    a) create a new note and insert it into the "deleted" node of network
    b) restore that note (force an exception)
    c) Listen for success msg CACHE_ERROR_UNKNOWN from flow
    d) confirm note is not in the cache
    e) confirm note is not in the network "notes" node
    f) confirm note is in the network "deletes" node
 */
class RestoreDeletedNoteTest {

    // system in test
    private val restoreDeletedNote: RestoreDeletedNote

    // dependencies
    private val dependencyContainer: DependencyContainer
    private val noteCacheDataSource: NoteCacheDataSource
    private val noteNetworkDataSource: NoteNetworkDataSource
    private val noteFactory: NoteFactory

    init {
        dependencyContainer = DependencyContainer()
        dependencyContainer.build()
        noteCacheDataSource = dependencyContainer.noteCacheDataSource
        noteNetworkDataSource = dependencyContainer.noteNetworkDataSource
        noteFactory = dependencyContainer.noteFactory
        restoreDeletedNote = RestoreDeletedNote(
            noteCacheDataSource = noteCacheDataSource,
            noteNetworkDataSource = noteNetworkDataSource
        )
    }

    @Test
    fun restoreNote_success_confirmCacheAndNetworkUpdated() = runBlocking {

        // Create a new note and insert into the 'deletes' node
        val restoredNode = noteFactory.createSingleNote(
            id = UUID.randomUUID().toString(),
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString()
        )
        noteNetworkDataSource.insertDeletedNote(restoredNode)

        // Confirm that note is in the 'deletes' node before restoration
        var deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue{deletedNotes.contains(restoredNode)}


        restoreDeletedNote.restoreDeletedNote(
            note = restoredNode,
            stateEvent = NoteListStateEvent.RestoreDeletedNoteEvent(restoredNode)
        ).collect(object : FlowCollector<DataState<NoteListViewState>?>{
            override suspend fun emit(value: DataState<NoteListViewState>?) {
                assertEquals(
                    value?.stateMessage?.response?.message,
                    RESTORE_NOTE_SUCCESS
                )
            }
        })

        // Confirm note is in the cache
        val noteInCache = noteCacheDataSource.searchNoteById(restoredNode.id)
        assertTrue{noteInCache == restoredNode}
        // Confirm note is in the network 'notes' node
        val noteInNetwork = noteNetworkDataSource.searchNote(restoredNode)
        assertTrue{noteInNetwork == restoredNode}
        // Confirm note is NOT in the network 'deletes' node
        deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertFalse{ deletedNotes.contains(restoredNode) }
    }

    @UseExperimental(InternalCoroutinesApi::class)
    @Test
    fun restoreNote_fail_confirmCacheAndNetworkUnchanged() = runBlocking {
        // Create a new note and insert into the 'deletes' node
        val restoredNode = noteFactory.createSingleNote(
            id = UUID.randomUUID().toString(),
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString()
        )
        noteNetworkDataSource.insertDeletedNote(restoredNode)

        // Confirm that note is in the 'deletes' node before restoration
        var deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue{deletedNotes.contains(restoredNode)}


        restoreDeletedNote.restoreDeletedNote(
            note = restoredNode,
            stateEvent = NoteListStateEvent.RestoreDeletedNoteEvent(restoredNode)
        ).collect(object : FlowCollector<DataState<NoteListViewState>?>{
            override suspend fun emit(value: DataState<NoteListViewState>?) {
                assertEquals(
                    value?.stateMessage?.response?.message,
                    RESTORE_NOTE_FAILED
                )
            }
        })

        // Confirm note is NOT in cache
        val noteInCache = noteCacheDataSource.searchNoteById(restoredNode.id)
        assertFalse{noteInCache == restoredNode}

        // Confirm note is NOT in the network 'notes' node
        val noteInNetwork = noteNetworkDataSource.searchNote(restoredNode)
        assertFalse{noteInNetwork == restoredNode}
        // Confirm note is in the network 'deletes' node
        deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue{ deletedNotes.contains(restoredNode) }

    }

    @Test
    fun throwException_checkGenericError_confirmNetworkAndCacheUnchanged() = runBlocking {
        // Create a new note and insert into the 'deletes' node
        val restoredNode = noteFactory.createSingleNote(
            id = UUID.randomUUID().toString(),
            title = UUID.randomUUID().toString(),
            body = UUID.randomUUID().toString()
        )
        noteNetworkDataSource.insertDeletedNote(restoredNode)

        // Confirm that note is in the 'deletes' node before restoration
        var deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue{deletedNotes.contains(restoredNode)}


        restoreDeletedNote.restoreDeletedNote(
            note = restoredNode,
            stateEvent = NoteListStateEvent.RestoreDeletedNoteEvent(restoredNode)
        ).collect(object : FlowCollector<DataState<NoteListViewState>?>{
            override suspend fun emit(value: DataState<NoteListViewState>?) {
                assertEquals(
                    value?.stateMessage?.response?.message,
                    RESTORE_NOTE_FAILED
                )
            }
        })

        // Confirm note is NOT in cache
        val noteInCache = noteCacheDataSource.searchNoteById(restoredNode.id)
        assertFalse{noteInCache == restoredNode}

        // Confirm note is NOT in the network 'notes' node
        val noteInNetwork = noteNetworkDataSource.searchNote(restoredNode)
        assertFalse{noteInNetwork == restoredNode}
        // Confirm note is in the network 'deletes' node
        deletedNotes = noteNetworkDataSource.getDeletedNotes()
        assertTrue{ deletedNotes.contains(restoredNode) }
    }


}