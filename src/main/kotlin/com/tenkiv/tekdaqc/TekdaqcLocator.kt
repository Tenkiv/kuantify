package com.tenkiv.tekdaqc

import com.tenkiv.FoundDevice
import com.tenkiv.LostDevice
import com.tenkiv.daqc.networking.DeviceLocator
import com.tenkiv.tekdaqc.hardware.ATekdaqc
import com.tenkiv.tekdaqc.locator.Locator
import com.tenkiv.tekdaqc.locator.OnTekdaqcDiscovered

class TekdaqcLocator : OnTekdaqcDiscovered, DeviceLocator<TekdaqcBoard>() {

    init {
        Locator.instance.addLocatorListener(this)
    }

    override fun onTekdaqcResponse(tekdaqc: ATekdaqc) {}

    override fun onTekdaqcNoLongerLocated(tekdaqc: ATekdaqc) {
        val board = activeDevices.filter { it.tekdaqc.serialNumber == tekdaqc.serialNumber }.firstOrNull()
        if (board != null) {
            broadcastChannel.offer(LostDevice(board))
        }
        activeDevices.removeIf { it.tekdaqc.serialNumber == tekdaqc.serialNumber }
    }

    override fun onTekdaqcFirstLocated(tekdaqc: ATekdaqc) {
        val board = TekdaqcBoard(tekdaqc)
        activeDevices.add(board)
        broadcastChannel.offer(FoundDevice(board))
    }

    override fun search() {
        Locator.instance.searchForTekdaqcs()
    }

    override fun stop() {
        Locator.instance.cancelLocator()
    }

}