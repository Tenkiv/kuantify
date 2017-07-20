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
import java.time.Duration

class TekdaqcDevice(val wrappedTekdaqc: ATekdaqc) : ControlDevice, DataAcquisitionDevice {

    override val inetAddr: InetAddress = InetAddress.getByName(wrappedTekdaqc.hostIP)
    override val serialNumber: String = wrappedTekdaqc.serialNumber
    override var isConnected = false
    override var networkProtocol: NetworkProtocol = NetworkProtocol.TELNET
    override var networkSharingStatus: SharingStatus = SharingStatus.NONE

    var lineFrequency: LineNoiseFrequency = LineNoiseFrequency.AccountFor(50.hertz)

    override fun connect(lineFrequency: LineNoiseFrequency, protocol: NetworkProtocol?) {

        this.lineFrequency = lineFrequency

        when (protocol) {
            NetworkProtocol.TELNET, NetworkProtocol.TCP -> {
                wrappedTekdaqc.connect(ATekdaqc.AnalogScale.ANALOG_SCALE_5V, ATekdaqc.CONNECTION_METHOD.ETHERNET)
            }
            else -> {
                //TODO: Add message
                throw UnsupportedProtocolException()
            }
        }

        initializeDevice()
    }

    override fun disconnect(protocol: NetworkProtocol?) {
        wrappedTekdaqc.disconnectCleanly()
    }

    fun restoreTekdaqc(timeout: Duration, reAddChannels: Boolean = true) {
        wrappedTekdaqc.restoreTekdaqc(timeout.toMillis(), reAddChannels)
    }

    override val analogOutputs: List<AnalogOutput> = emptyList()

    override val digitalOutputs: List<DigitalOutput> = toDaqcDO(wrappedTekdaqc.digitalOutputs.values)

    override val analogInputs: List<AnalogInput> = toDaqcAI(wrappedTekdaqc.analogInputs.values)

    override val digitalInputs: List<DigitalInput> = toDaqcDI(wrappedTekdaqc.digitalInputs.values)

    override val sharedOutputs: MutableMap<SharingStatus, Output<DaqcValue>> = HashMap()

    override val sharedInputs: MutableMap<SharingStatus, Input<DaqcValue>> = HashMap()

    override val hasAnalogOutputs get() = false

    override val hasDigitalOutputs get() = true

    override val hasAnalogInputs get() = true

    override val hasDigitalInputs get() = true

    val is400vManditory get() = mandatory400Voltage

    internal var mandatory400Voltage: Boolean = false

    override fun initializeDevice() {
        analogInputs.forEach { it.deactivate() }
        digitalInputs.forEach { it.deactivate() }
        digitalOutputs.forEach { it.deactivate() }
        wrappedTekdaqc.sample(0)
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
        get() = wrappedTekdaqc.analogInputScale
        set(value) {
            wrappedTekdaqc.analogInputScale = value
        }
}