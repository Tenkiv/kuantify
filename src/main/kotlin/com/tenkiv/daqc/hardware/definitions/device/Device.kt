package com.tenkiv.daqc.hardware.definitions.device

import com.tenkiv.daqc.LineNoiseFrequency
import com.tenkiv.daqc.networking.NetworkProtocol
import com.tenkiv.daqc.networking.SharingStatus
import java.net.InetAddress

interface Device {

    val inetAddr: InetAddress

    val serialNumber: String

    var isConnected: NetworkProtocol?

    var networkSharingStatus: SharingStatus

    fun connect(lineFrequency: LineNoiseFrequency, protocol: NetworkProtocol?)

    fun disconnect(protocol: NetworkProtocol?)

    fun initializeBoard()
}