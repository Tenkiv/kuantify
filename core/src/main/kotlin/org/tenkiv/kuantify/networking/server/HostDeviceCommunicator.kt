package org.tenkiv.kuantify.networking.server

import kotlinx.coroutines.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import kotlin.coroutines.*

abstract class HostDeviceCommunicator(scope: CoroutineScope, val device: LocalDevice) : CoroutineScope {

    private val job = Job(scope.coroutineContext[Job])

    override val coroutineContext: CoroutineContext = scope.coroutineContext + job

    fun cancel() {

        job.cancel()
    }

}