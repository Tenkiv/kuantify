package org.tenkiv.daqc.tekdaqc

import com.tenkiv.tekdaqc.hardware.ATekdaqc
import com.tenkiv.tekdaqc.locator.Locator
import com.tenkiv.tekdaqc.locator.OnTekdaqcDiscovered
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.daqc.FoundDevice
import org.tenkiv.daqc.LocatorUpdate
import org.tenkiv.daqc.LostDevice
import org.tenkiv.daqc.networking.DeviceLocator
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList

class TekdaqcLocator : DeviceLocator() {

    private val _activeDevices = CopyOnWriteArrayList<TekdaqcDevice>()

    override val activeDevices: List<TekdaqcDevice>
        get() = _activeDevices

    private val _broadcastChannel = ConflatedBroadcastChannel<LocatorUpdate<TekdaqcDevice>>()

    override val broadcastChannel: ConflatedBroadcastChannel<out LocatorUpdate<TekdaqcDevice>>
        get() = _broadcastChannel

    init {
        Locator.instance.addLocatorListener(object : OnTekdaqcDiscovered {
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
        })
    }

    override fun getDeviceForSerial(serialNumber: String): TekdaqcDevice? =
            activeDevices.firstOrNull { it.serialNumber == serialNumber }

    override fun awaitSpecificDevice(serialNumber: String, timeout: Duration?): Deferred<TekdaqcDevice> =
            _awaitSpecificDevice(serialNumber, timeout)

    override fun search() {
        Locator.instance.searchForTekdaqcs()
    }

    override fun stop() {
        Locator.instance.cancelLocator()
    }
}