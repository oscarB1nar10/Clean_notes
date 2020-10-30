package com.codingwithmitch.cleannotes.business.interactors.notelist

import com.codingwithmitch.cleannotes.business.data.cache.Abstraction.NoteCacheDataSource
import com.codingwithmitch.cleannotes.business.data.cache.CacheResponseHandler
import com.codingwithmitch.cleannotes.business.data.network.abstraction.NoteNetworkDataSource
import com.codingwithmitch.cleannotes.business.data.util.safeApiCall
import com.codingwithmitch.cleannotes.business.data.util.safeCacheCall
import com.codingwithmitch.cleannotes.business.domain.model.Note
import com.codingwithmitch.cleannotes.business.domain.state.*
import com.codingwithmitch.cleannotes.framework.Presentation.notelist.state.NoteListViewState
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RestoreDeletedNote(
    private val noteCacheDataSource: NoteCacheDataSource,
    private val noteNetworkDataSource: NoteNetworkDataSource
) {

    fun restoreDeletedNote(
        note: Note,
        stateEvent: StateEvent
    ): Flow<DataState<NoteListViewState>?> = flow{

        val cacheResult = safeCacheCall(IO){
            noteCacheDataSource.insertNote(note)
        }

        val response = object: CacheResponseHandler<NoteListViewState,Long>(
            response = cacheResult,
            stateEvent = stateEvent
        ){
            override fun handleSuccess(resultObj: Long): DataState<NoteListViewState>? {
                return if(resultObj > 0){
                    val viewState = NoteListViewState(
                        notePendingDelete = NoteListViewState.NotePendingDelete(
                            note
                        )
                    )
                    DataState.data(
                        response = Response(
                            message = RESTORE_NOTE_SUCCESS,
                            uiComponentType = UIComponentType.Toast(),
                            messageType = MessageType.Success()

                        ),
                        data = viewState,
                        stateEvent = stateEvent
                    )
                }else{
                    DataState.data(
                        response = Response(
                            message = RESTORE_NOTE_FAILED,
                            uiComponentType = UIComponentType.Toast(),
                            messageType = MessageType.Success()

                        ),
                        data = null,
                        stateEvent = stateEvent
                    )
                }
            }
        }.getResult()

        emit(response)

        updateNetwork(response = response?.stateMessage?.response?.message, note = note)

    }

    private suspend fun updateNetwork(response: String?, note: Note){
        if(response == "RESTORE_NOTE_SUCCESS"){
            // Insert into the 'notes' node
            safeApiCall(IO){
                noteNetworkDataSource.insertOrUpdateNote(note)
            }
            // Remove from 'deletes' node
            safeApiCall(IO){
                noteNetworkDataSource.deleteDeletedNote(note)
            }
        }
    }

    companion object{
        val RESTORE_NOTE_SUCCESS = "Successfully restored the deleted note."
        val RESTORE_NOTE_FAILED = "Failed to restore the deleted note."
    }
}