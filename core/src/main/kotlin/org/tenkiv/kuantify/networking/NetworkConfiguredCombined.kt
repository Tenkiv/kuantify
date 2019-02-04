package org.tenkiv.kuantify.networking

import org.tenkiv.kuantify.networking.configuration.*

interface NetworkConfiguredCombined {

    fun combinedConfig(config: CombinedRouteConfig)

}

interface NetworkConfiguredLocal {

    fun localConfig(config: SideRouteConfig)

}

interface NetworkConfiguredRemote {

    fun remoteConfig(config: SideRouteConfig)

}

fun Iterable<NetworkConfiguredLocal>.applyLocalConfigsTo(config: SideRouteConfig) {
    forEach {
        it.localConfig(config)
    }
}

fun Iterable<NetworkConfiguredRemote>.applyRemoteConfigsTo(config: SideRouteConfig) {
    forEach {
        it.remoteConfig(config)
    }
}