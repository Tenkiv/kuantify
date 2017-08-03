package org.tenkiv.daqc.hardware.definitions.device

import org.tenkiv.daqc.LineNoiseFrequency
import org.tenkiv.daqc.hardware.definitions.channel.QuantityInput
import org.tenkiv.daqc.networking.NetworkProtocol
import org.tenkiv.daqc.networking.SharingStatus
import java.net.InetAddress
import javax.measure.quantity.Temperature

interface Device {

    val inetAddr: InetAddress

    val serialNumber: String

    val temperatureReference: QuantityInput<Temperature>

    var isConnected: Boolean

    var networkProtocol: NetworkProtocol

    var networkSharingStatus: SharingStatus

    fun connect(lineFrequency: LineNoiseFrequency, protocol: NetworkProtocol? = null)

    fun disconnect(protocol: NetworkProtocol?)

    fun initializeDevice()
}