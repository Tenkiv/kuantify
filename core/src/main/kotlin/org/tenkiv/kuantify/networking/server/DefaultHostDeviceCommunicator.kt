package org.tenkiv.kuantify.networking.server

import kotlinx.coroutines.*
import org.tenkiv.kuantify.gate.*

class DefaultHostDeviceCommunicator(scope: CoroutineScope) : HostDeviceCommunicator(scope) {

    val ioStrands: Map<String, IOStrand<*>> = run {
        val map = HashMap<String, IOStrand<*>>()



        map
    }

}