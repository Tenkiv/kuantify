package org.tenkiv.tekdaqc

import com.tenkiv.tekdaqc.hardware.ATekdaqc
import com.tenkiv.tekdaqc.locator.Locator
import com.tenkiv.tekdaqc.locator.OnTekdaqcDiscovered
import org.tenkiv.FoundDevice
import org.tenkiv.LostDevice
import org.tenkiv.daqc.networking.DeviceLocator

class TekdaqcLocator : OnTekdaqcDiscovered, DeviceLocator() {

    init {
        Locator.instance.addLocatorListener(this)
    }

    override fun onTekdaqcResponse(tekdaqc: ATekdaqc) {}

    override fun onTekdaqcNoLongerLocated(tekdaqc: ATekdaqc) {
        val board = activeDevices.filter { it.serialNumber == tekdaqc.serialNumber }.firstOrNull()
        if (board != null) {
            broadcastChannel.offer(LostDevice(board))
        }
        activeDevices.removeIf { it.serialNumber == tekdaqc.serialNumber }
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