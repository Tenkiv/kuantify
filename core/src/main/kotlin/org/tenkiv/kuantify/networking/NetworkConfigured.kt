package org.tenkiv.kuantify.networking

import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.networking.configuration.*

interface NetworkConfigured<D : BaseKuantifyDevice> {

    fun SideRouteConfig<D>.configure()

}

fun <D : BaseKuantifyDevice> NetworkConfigured<D>.applyConfigTo(config: SideRouteConfig<D>) {
    config.configure()
}

fun <D : BaseKuantifyDevice> Iterable<NetworkConfigured<D>>.applyConfigTo(config: SideRouteConfig<D>) {
    forEach {
        it.applyConfigTo(config)
    }
}