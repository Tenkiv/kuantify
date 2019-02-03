package org.tenkiv.kuantify.networking.configuration

import mu.*
import org.tenkiv.kuantify.gate.acquire.input.*

private val logger = KotlinLogging.logger {}

fun CombinedRouteConfig.addStandardConfig() {

    device.daqcGateMap.forEach { id, gate ->
        when (gate) {
            is Input<*> -> TODO()
            else -> logger.debug {
                """Gate $id on device $device is not of a type supported by standard routing,
                    | see if it can be modified to fit a supported type, or ensure custom routing
                    | is provided for it.""".trimMargin()
            }
        }
    }
}