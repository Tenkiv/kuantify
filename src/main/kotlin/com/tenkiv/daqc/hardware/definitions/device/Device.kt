package com.tenkiv.daqc.hardware.definitions.device

import com.tenkiv.daqc.networking.NetworkProtocol
import com.tenkiv.daqc.networking.SharingStatus
import java.net.InetAddress

/**
 * Created by tenkiv on 4/7/17.
 */
interface Device {

    val inetAddr: InetAddress

    val serialNumber: String

    var isConnected: NetworkProtocol?

    var networkSharingStatus: SharingStatus

    fun connect(protocol: NetworkProtocol?)

    fun disconnect(protocol: NetworkProtocol?)
}