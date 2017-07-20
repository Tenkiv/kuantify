package org.tenkiv.tekdaqc

import com.tenkiv.tekdaqc.hardware.ATekdaqc
import com.tenkiv.tekdaqc.locator.Locator
import com.tenkiv.tekdaqc.locator.OnTekdaqcDiscovered
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.FoundDevice
import org.tenkiv.LocatorUpdate
import org.tenkiv.LostDevice
import org.tenkiv.daqc.networking.DeviceLocator
import java.util.concurrent.CopyOnWriteArrayList

class TekdaqcLocator : OnTekdaqcDiscovered, DeviceLocator() {

    private val _activeDevices = CopyOnWriteArrayList<TekdaqcDevice>()

    override val activeDevices: List<TekdaqcDevice>
        get() = _activeDevices

    private val _broadcastChannel = ConflatedBroadcastChannel<LocatorUpdate<TekdaqcDevice>>()

    override val broadcastChannel: ConflatedBroadcastChannel<out LocatorUpdate<TekdaqcDevice>>
        get() = _broadcastChannel

    init {
        Locator.instance.addLocatorListener(this)
    }

    override fun onTekdaqcResponse(tekdaqc: ATekdaqc) {}

    override fun onTekdaqcNoLongerLocated(tekdaqc: ATekdaqc) {
        val board = activeDevices.filter { it.serialNumber == tekdaqc.serialNumber }.firstOrNull()
        if (board != null) {
            _broadcastChannel.offer(LostDevice(board))
        }
        _activeDevices.removeIf { it.serialNumber == tekdaqc.serialNumber }
    }

    override fun onTekdaqcFirstLocated(tekdaqc: ATekdaqc) {
        val board = TekdaqcDevice(tekdaqc)
        _activeDevices.add(board)
        _broadcastChannel.offer(FoundDevice(board))
    }

    override fun awaitSpecificDevice(serialNumber: String): Deferred<TekdaqcDevice> = _awaitSpecificDevice(serialNumber)

    override fun search() {
        Locator.instance.searchForTekdaqcs()
    }

    override fun stop() {
        Locator.instance.cancelLocator()
    }

}