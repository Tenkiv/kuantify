package org.tenkiv.daqc.hardware.definitions.device

import org.tenkiv.daqc.LineNoiseFrequency
import org.tenkiv.daqc.networking.NetworkProtocol
import org.tenkiv.daqc.networking.SharingStatus
import java.net.InetAddress

interface Device {

    val inetAddr: InetAddress

    val serialNumber: String

    var isConnected: Boolean

    var networkProtocol: NetworkProtocol

    var networkSharingStatus: SharingStatus

    fun connect(lineFrequency: LineNoiseFrequency, protocol: NetworkProtocol?)

    fun disconnect(protocol: NetworkProtocol?)

    fun initializeBoard()
}