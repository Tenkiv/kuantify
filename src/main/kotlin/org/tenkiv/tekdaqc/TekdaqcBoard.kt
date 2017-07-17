package org.tenkiv.tekdaqc

import com.tenkiv.tekdaqc.hardware.ATekdaqc
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.LineNoiseFrequency
import org.tenkiv.daqc.hardware.definitions.channel.*
import org.tenkiv.daqc.hardware.definitions.device.ControlDevice
import org.tenkiv.daqc.hardware.definitions.device.DataAcquisitionDevice
import org.tenkiv.daqc.networking.NetworkProtocol
import org.tenkiv.daqc.networking.SharingStatus
import org.tenkiv.daqc.networking.UnsupportedProtocolException
import org.tenkiv.physikal.core.hertz
import java.net.InetAddress

class TekdaqcBoard(val tekdaqc: ATekdaqc) : ControlDevice, DataAcquisitionDevice {

    override val inetAddr: InetAddress = InetAddress.getByName(tekdaqc.hostIP)
    override val serialNumber: String = tekdaqc.serialNumber
    override var isConnected = false
    override var networkProtocol: NetworkProtocol = NetworkProtocol.TELNET
    override var networkSharingStatus: SharingStatus = SharingStatus.NONE

    var lineFrequency: LineNoiseFrequency = LineNoiseFrequency.AccountFor(50.hertz)

    override fun connect(lineFrequency: LineNoiseFrequency, protocol: NetworkProtocol?) {

        this.lineFrequency = lineFrequency

        when (protocol) {
            NetworkProtocol.TELNET, NetworkProtocol.TCP -> {
                tekdaqc.connect(ATekdaqc.AnalogScale.ANALOG_SCALE_5V, ATekdaqc.CONNECTION_METHOD.ETHERNET)
            }
            else -> {
                throw UnsupportedProtocolException()
            }
        }

        initializeBoard()
    }

    override fun disconnect(protocol: NetworkProtocol?) {
        tekdaqc.disconnectCleanly()
    }

    override val analogOutputs: List<AnalogOutput> = emptyList()

    override val digitalOutputs: List<DigitalOutput> = toDaqcDO(tekdaqc.digitalOutputs.values)

    override val analogInputs: List<AnalogInput> = toDaqcAI(tekdaqc.analogInputs.values)

    override val digitalInputs: List<DigitalInput> = toDaqcDI(tekdaqc.digitalInputs.values)

    override val sharedOutputs: MutableMap<SharingStatus, OutputCore<DaqcValue>> = HashMap()

    //override val sharedInputs: MutableMap<SharingStatus, Input<ValueInstant<DaqcValue>>> = HashMap()

    override val sharedInputs: MutableMap<SharingStatus, Input<DaqcValue>> = HashMap()

    override val hasAnalogOutputs get() = false

    override val hasDigitalOutputs get() = true

    override val hasAnalogInputs get() = true

    override val hasDigitalInputs get() = true

    val is400vManditory get() = mandatory400Voltage

    internal var mandatory400Voltage: Boolean = false

    override fun initializeBoard() {
        analogInputs.forEach { it.deactivate() }
        digitalInputs.forEach { it.deactivate() }
        digitalOutputs.forEach { it.deactivate() }
    }

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