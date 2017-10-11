package org.tenkiv.daqc.hardware.definitions.device

import org.tenkiv.daqc.LineNoiseFrequency
import org.tenkiv.daqc.hardware.definitions.QuantityInput
import org.tenkiv.daqc.networking.NetworkProtocol
import org.tenkiv.daqc.networking.SharingStatus
import java.net.InetAddress
import javax.measure.quantity.Temperature

interface Device {

    val inetAddr: InetAddress

    val serialNumber: String

    val temperatureReference: QuantityInput<Temperature>

    /**
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    val isConnected: Boolean

    /**
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    var networkProtocol: NetworkProtocol

    /**
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    var networkSharingStatus: SharingStatus

    fun connect(lineFrequency: LineNoiseFrequency, protocol: NetworkProtocol? = null)

    fun disconnect(protocol: NetworkProtocol?)

    fun initializeDevice()
}