package org.tenkiv.daqc.tekdaqc

import com.tenkiv.tekdaqc.hardware.ATekdaqc
import com.tenkiv.tekdaqc.utility.CriticalErrorListener
import com.tenkiv.tekdaqc.utility.TekdaqcCriticalError
import org.tenkiv.daqc.DaqcCriticalError
import org.tenkiv.daqc.DaqcValue
import org.tenkiv.daqc.LineNoiseFrequency
import org.tenkiv.daqc.daqcCriticalErrorBroadcastChannel
import org.tenkiv.daqc.hardware.definitions.Input
import org.tenkiv.daqc.hardware.definitions.Output
import org.tenkiv.daqc.hardware.definitions.QuantityInput
import org.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import org.tenkiv.daqc.hardware.definitions.channel.AnalogOutput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import org.tenkiv.daqc.hardware.definitions.device.ControlDevice
import org.tenkiv.daqc.hardware.definitions.device.DataAcquisitionDevice
import org.tenkiv.daqc.networking.NetworkProtocol
import org.tenkiv.daqc.networking.SharingStatus
import org.tenkiv.daqc.networking.UnsupportedProtocolException
import org.tenkiv.physikal.core.hertz
import java.net.InetAddress
import java.time.Duration
import javax.measure.quantity.Temperature

class TekdaqcDevice(val wrappedTekdaqc: ATekdaqc) : ControlDevice, DataAcquisitionDevice, CriticalErrorListener {

    override val temperatureReference: QuantityInput<Temperature> =
            TekdaqcTemperatureReference(TekdaqcAnalogInput(this, wrappedTekdaqc.temperatureReference))

    override val inetAddr: InetAddress get() = InetAddress.getByName(wrappedTekdaqc.hostIP)
    override val serialNumber: String get() = wrappedTekdaqc.serialNumber
    @Volatile override var isConnected = false
    @Volatile override var networkProtocol: NetworkProtocol = NetworkProtocol.TELNET //TODO IMPLEMENT ISSUE #1 & #8
    @Volatile override var networkSharingStatus: SharingStatus = SharingStatus.NONE //TODO IMPLEMENT ISSUE #1 & #8

    var lineFrequency: LineNoiseFrequency = LineNoiseFrequency.AccountFor(60.hertz)

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

    override fun onTekdaqcCriticalError(criticalError: TekdaqcCriticalError) {
        when (criticalError) {
            TekdaqcCriticalError.FAILED_TO_REINITIALIZE -> {
                daqcCriticalErrorBroadcastChannel.offer(DaqcCriticalError.FailedToReinitialize(this))
            }
            TekdaqcCriticalError.FAILED_MAJOR_COMMAND -> {
                daqcCriticalErrorBroadcastChannel.offer(DaqcCriticalError.FailedMajorCommand(this))
            }
            TekdaqcCriticalError.PARTIAL_DISCONNECTION -> {
                daqcCriticalErrorBroadcastChannel.offer(DaqcCriticalError.PartialDisconnection(this))
            }
            TekdaqcCriticalError.TERMINAL_CONNECTION_DISRUPTION -> {
                daqcCriticalErrorBroadcastChannel.offer(DaqcCriticalError.TerminalConnectionDisruption(this))
            }
        }
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
        wrappedTekdaqc.addCriticalFailureListener(this)
        wrappedTekdaqc.readAnalogInput(36, 5)
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
        get() = wrappedTekdaqc.analogScale
        set(value) {
            wrappedTekdaqc.analogScale = value
        }
}