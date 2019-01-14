package org.tenkiv.kuantify.networking.server

import kotlinx.coroutines.*
import kotlin.coroutines.*

abstract class HostDeviceCommunicator(scope: CoroutineScope) : CoroutineScope {

    private val job = Job(scope.coroutineContext[Job])

    override val coroutineContext: CoroutineContext = scope.coroutineContext + job

    fun cancel() {

        job.cancel()
    }

}