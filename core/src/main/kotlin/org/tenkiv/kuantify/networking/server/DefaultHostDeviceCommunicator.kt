package org.tenkiv.kuantify.networking.server

import kotlinx.coroutines.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.hardware.definitions.device.*

class DefaultHostDeviceCommunicator(scope: CoroutineScope, device: LocalDevice) :
    HostDeviceCommunicator(scope, device) {

    val ioStrands: Map<String, IOStrand<*>> = run {
        val map = HashMap<String, IOStrand<*>>()



        map
    }

}