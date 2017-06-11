package com.tenkiv.tekdaqc

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.channel.*
import com.tenkiv.daqc.hardware.definitions.device.ControlDevice
import com.tenkiv.daqc.hardware.definitions.device.DataAquisitionDevice
import com.tenkiv.daqc.networking.NetworkProtocol
import com.tenkiv.daqc.networking.SharingStatus
import com.tenkiv.daqc.networking.UnsupportedConnectionProtocolException
import com.tenkiv.tekdaqc.hardware.ATekdaqc
import java.net.InetAddress
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by tenkiv on 5/26/17.
 */
class TekdaqcBoard(val tekdaqc: ATekdaqc) : ControlDevice, DataAquisitionDevice {

    override val inetAddr: InetAddress = InetAddress.getByName(tekdaqc.hostIP)

    override var isConnected: NetworkProtocol? = null

    override var networkSharingStatus: SharingStatus = SharingStatus.NONE

    override fun connect(protocol: NetworkProtocol?) {
        when (protocol) {
            NetworkProtocol.TELNET, NetworkProtocol.TCP -> {
                tekdaqc.connect(ATekdaqc.AnalogScale.ANALOG_SCALE_5V, ATekdaqc.CONNECTION_METHOD.ETHERNET)
            }
            else -> {
                throw UnsupportedConnectionProtocolException()
            }
        }
    }

    override fun disconnect(protocol: NetworkProtocol?) {
        tekdaqc.disconnectCleanly()
    }

    override val analogOutputs: List<AnalogOutput> = emptyList()

    override val digitalOutputs: List<DigitalOutput>
            = CopyOnWriteArrayList<DigitalOutput>(toDaqcDO(tekdaqc.digitalOutputs.values))

    override val analogInputs: List<AnalogInput>
            = CopyOnWriteArrayList<AnalogInput>(toDaqcAI(tekdaqc.analogInputs.values))

    override val digitalInputs: List<DigitalInput>
            = CopyOnWriteArrayList<DigitalInput>(toDaqcDI(tekdaqc.digitalInputs.values))

    override val sharedOutputs: MutableMap<SharingStatus, Output<DaqcValue>> = HashMap()

    override val sharedInputs: MutableMap<SharingStatus, Input<DaqcValue>> = HashMap()

    override fun hasAnalogOutputs(): Boolean = false

    override fun hasDigitalOutputs(): Boolean = true

    override fun hasAnalogInputs(): Boolean = true

    override fun hasDigitalInputs(): Boolean = true

    private fun toDaqcAI(inputs: Collection<com.tenkiv.tekdaqc.hardware.AAnalogInput>): List<TekdaqcAnalogInput> {
        val tAI = ArrayList<TekdaqcAnalogInput>()
        inputs.forEach { tAI.add(TekdaqcAnalogInput(this, it)) }
        return tAI
    }

    private fun toDaqcDI(inputs: Collection<com.tenkiv.tekdaqc.hardware.DigitalInput>): List<TekdaqcDigitalInput> {
        val tDI = ArrayList<TekdaqcDigitalInput>()
        inputs.forEach { tDI.add(TekdaqcDigitalInput(this, it)) }
        return tDI
    }

    private fun toDaqcDO(inputs: Collection<com.tenkiv.tekdaqc.hardware.DigitalOutput>): List<TekdaqcDigitalOutput> {
        val tDO = ArrayList<TekdaqcDigitalOutput>()
        inputs.forEach { tDO.add(TekdaqcDigitalOutput(this, it)) }
        return tDO
    }

    var analogScale: ATekdaqc.AnalogScale
        get() = tekdaqc.analogInputScale
        set(value) {
            tekdaqc.analogInputScale = value
        }
}