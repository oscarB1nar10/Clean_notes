package com.codingwithmitch.cleannotes.business.domain.state

import com.codingwithmitch.cleannotes.util.printLogD
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

@FlowPreview
@ExperimentalCoroutinesApi
abstract class DataChannelManager<ViewState> {

    private val dataChannel = BroadcastChannel<DataState<ViewState>>(Channel.BUFFERED)
    private var channelScope: CoroutineScope? = null
    private val stateEventManager: StateEventManager = StateEventManager()

    val messageStack = MessageStack()

    val shouldDisplayProgressBar = stateEventManager.shouldDisplayProgressBar

    fun setupChannel(){
        cancelJobs()
        initChannel()
    }

    //region to handle data consumed from server or local DB
    private fun initChannel(){
        dataChannel
            .asFlow()
            .onEach{ dataState ->
                withContext(Main){
                    dataState.data?.let { data ->
                        handleNewData(data)
                    }
                    dataState.stateMessage?.let { stateMessage ->
                        handleNewStateMessage(stateMessage)
                    }
                    dataState.stateEvent?.let { stateEvent ->
                        removeStateEvent(stateEvent)
                    }
                }
            }
            .launchIn(getChannelScope())
    }

    abstract fun handleNewData(data: ViewState)

    private fun handleNewStateMessage(stateMessage: StateMessage){
        appendStateMessage(stateMessage)
    }

    private fun appendStateMessage(stateMessage: StateMessage) {
        messageStack.add(stateMessage)
    }

    fun removeStateEvent(stateEvent: StateEvent?)
            = stateEventManager.removeStateEvent(stateEvent)

    //endregion

    //region launch job

    fun launchJob(
        stateEvent: StateEvent,
        jobFunction: Flow<DataState<ViewState>?>
    ){
        if(!isStateEventActive(stateEvent)){
            printLogD("DCM", "launching job: ${stateEvent.eventName()}")
            addStateEvent(stateEvent)
            jobFunction
                .onEach { dataState ->
                    dataState?.let { dState ->
                        offerToDataChannel(dState)
                    }
                }
                .launchIn(getChannelScope())
        }
    }

    private fun isStateEventActive(stateEvent: StateEvent)
            = stateEventManager.isStateEventActive(stateEvent)

    private fun addStateEvent(stateEvent: StateEvent)
            = stateEventManager.addStateEvent(stateEvent)

    private fun offerToDataChannel(dataState: DataState<ViewState>){
        dataChannel.let {
            if(!it.isClosedForSend){
                it.offer(dataState)
            }
        }
    }

    fun getChannelScope(): CoroutineScope {
        return channelScope?: setupNewChannelScope(CoroutineScope(IO))
    }

    private fun setupNewChannelScope(coroutineScope: CoroutineScope): CoroutineScope{
        channelScope = coroutineScope
        return channelScope as CoroutineScope
    }

    //endregion

    //region cancel job
    fun cancelJobs(){
        if(channelScope != null){
            if(channelScope?.isActive == true){
                channelScope?.cancel()
            }
            channelScope = null
        }
        clearActiveStateEventCounter()
    }

    fun clearActiveStateEventCounter()
            = stateEventManager.clearActiveStateEventCounter()

    //endregion

    private fun isMessageStackEmpty(): Boolean {
        return messageStack.isStackEmpty()
    }

    fun clearStateMessage(index: Int = 0) = messageStack.removeAt(index)

    fun clearAllStateMessages() = messageStack.clear()

    fun printStateMessages(){
        for(message in messageStack){
            printLogD("DCM", "${message.response.message}")
        }
    }

    // for debugging
    fun getActiveJobs() = stateEventManager.getActiveJobNames()

    fun isJobAlreadyActive(stateEvent: StateEvent): Boolean {
        return isStateEventActive(stateEvent)
    }

}