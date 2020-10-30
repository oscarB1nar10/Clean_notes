package com.codingwithmitch.cleannotes.business.interactors.notelist

import com.codingwithmitch.cleannotes.business.data.cache.Abstraction.NoteCacheDataSource
import com.codingwithmitch.cleannotes.business.domain.model.NoteFactory
import com.codingwithmitch.cleannotes.business.domain.state.DataState
import com.codingwithmitch.cleannotes.business.interactors.notelist.GetNumNotes.Companion.GET_NUM_NOTES_SUCCESS
import com.codingwithmitch.cleannotes.di.DependencyContainer
import com.codingwithmitch.cleannotes.framework.Presentation.notelist.state.NoteListStateEvent
import com.codingwithmitch.cleannotes.framework.Presentation.notelist.state.NoteListStateEvent.*
import com.codingwithmitch.cleannotes.framework.Presentation.notelist.state.NoteListViewState
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test


/*
1) getNumNotes_success_confirmCorrect()
    a) get the number of notes in cache.
    b) listen for GET_NUM_NOTES_SUCCESS from flow emission.
    c) compare with number of notes in the fake data set.
 */
@InternalCoroutinesApi
class GetNumNotesTest {

    // System in test
    private val getNumNotes: GetNumNotes

    // dependencies
    private val dependencyContainer: DependencyContainer
    private val noteCacheDataSource: NoteCacheDataSource
    private val noteFactory: NoteFactory

    init {
        dependencyContainer = DependencyContainer()
        dependencyContainer.build()
        noteCacheDataSource = dependencyContainer.noteCacheDataSource
        noteFactory = dependencyContainer.noteFactory
        getNumNotes = GetNumNotes(
            noteCacheDataSource = noteCacheDataSource
        )
    }

    @Test
    fun getNumNotes_success_confirmCorrect() = runBlocking{
        var numNotes = 0
        getNumNotes.getNumNotes(
            stateEvent = GetNumNotesInCacheEvent()
        ).collect(object : FlowCollector<DataState<NoteListViewState>?>{
            override suspend fun emit(value: DataState<NoteListViewState>?) {
              assertEquals(
                  value?.stateMessage?.response?.message,
                  GET_NUM_NOTES_SUCCESS
              )
                numNotes = value?.data?.numNotesInCache?:0
            }

        })
        val actualNumNotesInCache = noteCacheDataSource.getNumNotes()
        assertTrue{actualNumNotesInCache == numNotes}
    }
}