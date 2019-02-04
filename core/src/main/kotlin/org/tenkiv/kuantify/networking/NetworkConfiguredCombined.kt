package org.tenkiv.kuantify.networking

import org.tenkiv.kuantify.networking.configuration.*

interface NetworkConfiguredCombined {

    fun combinedConfig(config: CombinedRouteConfig)

}

interface NetworkConfiguredSide {

    fun sideConfig(config: SideRouteConfig)

}

fun Iterable<NetworkConfiguredCombined>.applyCombinedConfigsTo(config: CombinedRouteConfig) {
    forEach {
        it.combinedConfig(config)
    }
}

fun Iterable<NetworkConfiguredSide>.applySideConfigsTo(config: SideRouteConfig) {
    forEach {
        it.sideConfig(config)
    }
}