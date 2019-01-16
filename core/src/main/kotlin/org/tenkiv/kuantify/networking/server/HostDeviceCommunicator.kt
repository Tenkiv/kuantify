package org.tenkiv.kuantify.networking.server

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import kotlin.coroutines.*

abstract class HostDeviceCommunicator(scope: CoroutineScope, val device: LocalDevice) : CoroutineScope {

    private val job = Job(scope.coroutineContext[Job])

    override val coroutineContext: CoroutineContext = scope.coroutineContext + job

    init {
        launch {
            device.ioStrandMap.forEach { id, strand ->
                launch {
                    strand.updateBroadcaster.consumeEach {
                        when (it.value) {
                            is BinaryState -> it.value
                            is DaqcQuantity<*> -> it.value
                        }

                    }
                }
            }
        }

    }

    fun cancel() {

        job.cancel()
    }

}