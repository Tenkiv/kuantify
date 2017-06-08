package com.tenkiv.tekdaqc

import com.tenkiv.daqc.hardware.definitions.Updatable
import com.tenkiv.daqc.hardware.definitions.device.Device
import com.tenkiv.daqc.networking.RemoteLocator
import com.tenkiv.tekdaqc.hardware.ATekdaqc
import com.tenkiv.tekdaqc.locator.Locator
import com.tenkiv.tekdaqc.locator.OnTekdaqcDiscovered
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by tenkiv on 6/5/17.
 */
class TekdaqcLocator: OnTekdaqcDiscovered, RemoteLocator<List<TekdaqcBoard>>() {

    override val activeDevices: List<TekdaqcBoard> = CopyOnWriteArrayList<TekdaqcBoard>()

    init { Locator.instance.addLocatorListener(this) }

    override fun onTekdaqcResponse(tekdaqc: ATekdaqc) { }

    override fun onTekdaqcNoLongerLocated(tekdaqc: ATekdaqc) {
        println("Tekdaqc Not Located")
        (activeDevices as? MutableList)?.removeIf { it.tekdaqc.serialNumber == tekdaqc.serialNumber }
        latestValue = activeDevices
    }

    override fun onTekdaqcFirstLocated(tekdaqc: ATekdaqc) {
        println("Tekdaqc Located ${tekdaqc.serialNumber}")
        val board = TekdaqcBoard(tekdaqc)
        println("Pre Active Devices? $activeDevices Board $board")
        (activeDevices as? MutableList)?.add(board)

        println("Active Devices? $activeDevices Board $board")

        latestValue = activeDevices
    }

    override fun search() { Locator.instance.searchForTekdaqcs() }

    override fun stop() { Locator.instance.cancelLocator() }

}