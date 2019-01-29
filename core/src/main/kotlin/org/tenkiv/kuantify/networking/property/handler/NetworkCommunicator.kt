package org.tenkiv.kuantify.networking.property.handler

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.gate.control.output.*
import org.tenkiv.kuantify.hardware.definitions.channel.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.server.*
import org.tenkiv.physikal.core.*
import tec.units.indriya.*
import java.util.*
import javax.measure.quantity.*
import kotlin.coroutines.*

open class NetworkCommunicator(
    val device: KuantifyDevice
) : CoroutineScope {

    private val parentJob: Job? = device.coroutineContext[Job]

    @Volatile
    private var job: Job = Job(parentJob)

    final override val coroutineContext: CoroutineContext get() = device.coroutineContext + Dispatchers.Daqc + job

    protected val daqcGateMap get() = device.daqcGateMap

    protected open val additionalDataChannels: Map<List<String>, Channel<String?>>? = null

    private val updateIgnoreMap: IdentityHashMap<Trackable<*>, Boolean> = IdentityHashMap()

    open fun start() {
        initDaqcGateSending()
    }

    fun stop() {
        job.cancel()
        job = Job(parentJob)
    }

    internal suspend fun send(route: List<String>, serializedValue: String?) {
        val message = Json.stringify(NetworkMessage.serializer(), NetworkMessage(route, serializedValue))

        when (device) {
            is LocalDevice -> ClientHandler.sendToAll(message)
            is RemoteKuantifyDevice -> device.sendChannel.send(message)
        }
    }

    private fun initDaqcGateSending() {
        daqcGateMap.forEach { gateId, gate ->

            initIsTransceivingSending(gateId, gate)

            when (gate) {
                is IOStrand<*> -> initIOStrandSending(gateId, gate)
                is DigitalChannel<*> -> initDigitalChannelSending(gateId, gate)
                is AnalogInput -> initAnalogInputSending(gateId, gate)
            }

        }
    }

    private fun initIsTransceivingSending(gateId: String, gate: DaqcGate<*>) {
        if (device is LocalDevice) launch(Dispatchers.Default) {
            val route = listOf(RC.DAQC_GATE, gateId, RC.IS_TRANSCEIVING)
            gate.isTransceiving.updateBroadcaster.consumeEach {
                val serializedValue = Json.stringify(BooleanSerializer, it)
                send(route, serializedValue)
            }
        }
    }

    private fun initIOStrandSending(gateId: String, strand: IOStrand<*>) {
        when (strand) {
            is Input<*> -> initInputSending(gateId, strand)
            is Output<*> -> initOutputValueSending(gateId, strand)
        }
    }

    private fun initInputSending(gateId: String, input: Input<*>) {
        initInputValueSending(gateId, input)
        initUpdateRateSending(gateId, input.updateRate)
    }

    private fun initInputValueSending(gateId: String, input: Input<*>) {
        if (device is LocalDevice) launch(Dispatchers.Default) {
            val route = listOf(RC.DAQC_GATE, gateId, RC.VALUE)
            input.updateBroadcaster.consumeEach {
                val value = when (val measurementValue = it.value) {
                    is BinaryState -> Json.stringify(BinaryState.serializer(), measurementValue)
                    is DaqcQuantity<*> -> Json.stringify(ComparableQuantitySerializer, measurementValue)
                }

                send(route, value)
            }
        }
    }

    private fun initUpdateRateSending(gateId: String, updateRate: UpdateRate) {
        if (updateRate is UpdateRate.Configured && device is LocalDevice) launch(Dispatchers.Default) {
            val route = listOf(RC.DAQC_GATE, gateId, RC.UPDATE_RATE)
            updateRate.updateBroadcaster.consumeEach {
                val value = Json.stringify(ComparableQuantitySerializer, it)
                send(route, value)
            }
        }
    }

    private fun initOutputValueSending(gateId: String, output: Output<*>) {
        launch(Dispatchers.Daqc) {
            val route = listOf(RC.DAQC_GATE, gateId, RC.VALUE)

            output.updateBroadcaster.consumeEach {
                if (!output.ignoreNextUpdate) {
                    val value = when (val measurementValue = it.value) {
                        is BinaryState -> Json.stringify(BinaryState.serializer(), measurementValue)
                        is DaqcQuantity<*> -> Json.stringify(ComparableQuantitySerializer, measurementValue)
                    }

                    send(route, value)
                } else {
                    output.ignoreNextUpdate = false
                }
            }
        }
    }

    private fun initDigitalChannelSending(gateId: String, channel: DigitalChannel<*>) {
        initAvgFrequencySending(gateId, channel)
        initIsTransceivingBinStateSending(gateId, channel)
        initIsTransceivingPwmSending(gateId, channel)
        initIsTransceivingFrequencySending(gateId, channel)

        when (channel) {
            is DigitalInput -> initDigitalInputSending(gateId, channel)
            is DigitalOutput -> initDigitalOutputSending(gateId, channel)
        }
    }

    private fun initDigitalInputSending(gateId: String, input: DigitalInput) {
        initUpdateRateSending(gateId, input.updateRate)
        initDigitalInputValueSending(gateId, input)
    }

    private fun initDigitalOutputSending(gateId: String, output: DigitalOutput) {
        initDigitalOutputValueSending(gateId, output)
    }

    private fun initDigitalInputValueSending(gateId: String, input: DigitalInput) {
        if (device is LocalDevice) launch(Dispatchers.Default) {
            val route = listOf(RC.DAQC_GATE, gateId, RC.VALUE)

            input.updateBroadcaster.consumeEach {
                val value = Json.stringify(ValueInstantSerializer(DigitalChannelValue.serializer()), it)
                send(route, value)
            }
        }
    }

    private fun initDigitalOutputValueSending(gateId: String, output: DigitalOutput) {
        launch(Dispatchers.Daqc) {
            val route = listOf(RC.DAQC_GATE, gateId, RC.VALUE)

            output.updateBroadcaster.consumeEach {
                if (!output.ignoreNextUpdate) {
                    val value = Json.stringify(ValueInstantSerializer(DigitalChannelValue.serializer()), it)
                    send(route, value)
                } else {
                    output.ignoreNextUpdate = false
                }
            }
        }
    }

    private fun initAvgFrequencySending(gateId: String, channel: DigitalChannel<*>) {
        launch(Dispatchers.Daqc) {
            val route = listOf(RC.DAQC_GATE, gateId, RC.AVG_FREQUENCY)

            channel.avgFrequency.updateBroadcaster.consumeEach {
                if (!channel.avgFrequency.ignoreNextUpdate) {
                    val value = Json.stringify(ComparableQuantitySerializer, it)
                    send(route, value)
                } else {
                    channel.avgFrequency.ignoreNextUpdate = false
                }
            }
        }
    }

    private fun initIsTransceivingBinStateSending(gateId: String, channel: DigitalChannel<*>) {
        if (device is LocalDevice) launch(Dispatchers.Default) {
            val route = listOf(RC.DAQC_GATE, gateId, RC.IS_TRANSCEIVING_BIN_STATE)

            channel.isTransceivingBinaryState.updateBroadcaster.consumeEach {
                val value = Json.stringify(BooleanSerializer, it)
                send(route, value)
            }
        }
    }

    private fun initIsTransceivingPwmSending(gateId: String, channel: DigitalChannel<*>) {
        if (device is LocalDevice) launch(Dispatchers.Default) {
            val route = listOf(RC.DAQC_GATE, gateId, RC.IS_TRANSCEIVING_PWM)

            channel.isTransceivingPwm.updateBroadcaster.consumeEach {
                val value = Json.stringify(BooleanSerializer, it)
                send(route, value)
            }
        }
    }

    private fun initIsTransceivingFrequencySending(gateId: String, channel: DigitalChannel<*>) {
        if (device is LocalDevice) launch(Dispatchers.Default) {
            val route = listOf(RC.DAQC_GATE, gateId, RC.IS_TRANSCEIVING_FREQUENCY)

            channel.isTransceivingFrequency.updateBroadcaster.consumeEach {
                val value = Json.stringify(BooleanSerializer, it)
                send(route, value)
            }
        }
    }

    private fun initAnalogInputSending(gateId: String, channel: AnalogInput) {
        initMaxAcceptableErrorSending(gateId, channel)
        initMaxElectricPotentialSending(gateId, channel)
    }

    private fun initMaxAcceptableErrorSending(gateId: String, channel: AnalogInput) {
        launch(Dispatchers.Daqc) {
            val route = listOf(RC.DAQC_GATE, gateId, RC.MAX_ACCEPTABLE_ERROR)

            channel.maxAcceptableError.updateBroadcaster.consumeEach {
                if (!channel.maxAcceptableError.ignoreNextUpdate) {
                    val value = Json.stringify(ComparableQuantitySerializer, it)
                    send(route, value)
                } else {
                    channel.maxAcceptableError.ignoreNextUpdate = false
                }
            }
        }
    }

    private fun initMaxElectricPotentialSending(gateId: String, channel: AnalogInput) {
        launch(Dispatchers.Daqc) {
            val route = listOf(RC.DAQC_GATE, gateId, RC.MAX_ELECTRIC_POTENTIAL)

            channel.maxElectricPotential.updateBroadcaster.consumeEach {
                if (!channel.maxElectricPotential.ignoreNextUpdate) {
                    val value = Json.stringify(ComparableQuantitySerializer, it)
                    send(route, value)
                } else {
                    channel.maxElectricPotential.ignoreNextUpdate = false
                }
            }
        }
    }

    //TODO: Check additional data channels if cast fails
    //TODO: Throw specific exceptions for errors in message reception, no !!
    internal suspend fun receiveMessage(route: List<String>, message: String?) {
        when (route.first()) {
            RC.DAQC_GATE -> receiveDaqcGateMsg(route.drop(1), message)
            else -> receiveOtherMessage(route, message)
        }
    }

    private suspend fun receiveDaqcGateMsg(route: List<String>, message: String?) {
        val gateId = route.first()
        val command = route.drop(1).first()

        when (command) {
            RC.BUFFER -> receiveBufferMsg(gateId, message)
            RC.MAX_ACCEPTABLE_ERROR -> receiveMaxErrorMsg(gateId, message)
            RC.MAX_ELECTRIC_POTENTIAL -> receiveMaxElectricPotential(gateId, message)
            RC.VALUE -> receiveValueMsg(gateId, message)
            RC.START_SAMPLING -> receiveStartSampling(gateId)
            RC.START_SAMPLING_BINARY_STATE -> receiveStartSamplingBinaryState(gateId)
            RC.START_SAMPLING_PWM -> receiveStartSamplingPwm(gateId)
            RC.START_SAMPLING_TRANSITION_FREQUENCY -> receiveStartSamplingTransitionFrequency(gateId)
            RC.STOP_TRANSCEIVING -> receiveStopTransceiving(gateId)
            RC.PULSE_WIDTH_MODULATE -> receivePulseWidthModulate(gateId, message)
            RC.SUSTAIN_TRANSITION_FREQUENCY -> receiveSustainTransitionFrequency(gateId, message)
            RC.AVG_FREQUENCY -> receiveAvgFrequency(gateId, message)
            else -> receiveOtherDaqcGateMessage(route, message)
        }
    }

    private fun receiveValueMsg(gateId: String, message: String?) {
        when (val output = daqcGateMap[gateId]) {
            is QuantityOutput<*> -> receiveQuantityOutputValueMsg(output, message)
            is BinaryStateOutput -> receiveBinaryStateOutputValueMsg(output, message)
            is DigitalOutput -> receiveDigitalOutputValueMsg(output, message)
        }
    }

    private fun receiveQuantityOutputValueMsg(output: QuantityOutput<*>, message: String?) {
        val setting = Json.parse(ComparableQuantitySerializer, message!!)
        output.unsafeSetOutput(setting)
    }

    private fun receiveBinaryStateOutputValueMsg(output: BinaryStateOutput, message: String?) {
        val setting = Json.parse(BinaryState.serializer(), message!!)
        output.setOutput(setting)
    }

    private fun receiveDigitalOutputValueMsg(output: DigitalOutput, message: String?) {
        val setting = Json.parse(DigitalChannelValue.serializer(), message!!)
        when (setting) {
            is DigitalChannelValue.BinaryState -> output.setOutputState(setting.state)
            is DigitalChannelValue.Percentage -> output.pulseWidthModulate(setting.percent)
            is DigitalChannelValue.Frequency -> output.sustainTransitionFrequency(setting.frequency)
        }
    }

    private fun receiveBufferMsg(gateId: String, message: String?) {
        val buffer: Boolean = Json.parse(BooleanSerializer, message!!)

        (daqcGateMap[gateId] as AnalogInput).buffer.set(buffer)
    }

    private fun receiveMaxErrorMsg(gateId: String, message: String?) {
        val maxAcceptableError: ComparableQuantity<ElectricPotential> =
            Json.parse(ComparableQuantitySerializer, message!!).asType()

        (daqcGateMap[gateId] as AnalogInput).maxAcceptableError.set(maxAcceptableError)
    }

    private fun receiveMaxElectricPotential(gateId: String, message: String?) {
        val maxElectricPotential: ComparableQuantity<ElectricPotential> =
            Json.parse(ComparableQuantitySerializer, message!!).asType()

        (daqcGateMap[gateId] as AnalogInput).maxElectricPotential.set(maxElectricPotential)
    }

    private fun receiveStartSampling(gateId: String) {
        (daqcGateMap[gateId] as Input<*>).startSampling()
    }

    private fun receiveStartSamplingBinaryState(gateId: String) {
        (daqcGateMap[gateId] as DigitalInput).startSamplingBinaryState()
    }

    private fun receiveStartSamplingPwm(gateId: String) {
        (daqcGateMap[gateId] as DigitalInput).startSamplingPwm()
    }

    private fun receiveStartSamplingTransitionFrequency(gateId: String) {
        (daqcGateMap[gateId] as DigitalInput).startSamplingTransitionFrequency()
    }

    private fun receiveStopTransceiving(gateId: String) {
        daqcGateMap[gateId]?.stopTransceiving()
    }

    private fun receivePulseWidthModulate(gateId: String, message: String?) {
        val percent: ComparableQuantity<Dimensionless> = Json.parse(ComparableQuantitySerializer, message!!).asType()

        (daqcGateMap[gateId] as DigitalOutput).pulseWidthModulate(percent, Output.DEFAULT_PANIC_ON_FAILURE)
    }

    private fun receiveSustainTransitionFrequency(gateId: String, message: String?) {
        val transitionFrequency: ComparableQuantity<Frequency> =
            Json.parse(ComparableQuantitySerializer, message!!).asType()

        (daqcGateMap[gateId] as DigitalOutput).sustainTransitionFrequency(
            transitionFrequency,
            Output.DEFAULT_PANIC_ON_FAILURE
        )
    }

    private fun receiveAvgFrequency(gateId: String, message: String?) {
        val avgFrequency: ComparableQuantity<Frequency> =
            Json.parse(ComparableQuantitySerializer, message!!).asType()

        (daqcGateMap[gateId] as DigitalChannel<*>).avgFrequency.set(avgFrequency)
    }

    private suspend fun receiveOtherMessage(route: List<String>, message: String?) {
        if (additionalDataChannels == null) {
            //TODO: Handle invalid message
        } else {
            additionalDataChannels?.get(route)?.send(message) ?: TODO("Handle invalid message")
        }
    }

    @Suppress("NAME_SHADOWING")
    private suspend fun receiveOtherDaqcGateMessage(route: List<String>, message: String?) {
        if (additionalDataChannels == null) {
            //TODO: Handle invalid message
        } else {
            val route = mutableListOf(RC.DAQC_GATE).apply { addAll(route) }
            additionalDataChannels?.get(route)?.send(message) ?: TODO("Handle invalid message")
        }
    }

    /**
     * Can be used to avoid ping ponging updates back and forth for properties that are sent and received by both
     * the local and remote device. This property is not thread safe and should only be accessed from
     * [Dispatchers.Daqc] (if you don't manually set dispatchers in your [NetworkCommunicator] you have nothing to
     * worry about).
     */
    protected var Trackable<*>.ignoreNextUpdate: Boolean
        get() = updateIgnoreMap[this] ?: false
        set(value) {
            updateIgnoreMap[this] = value
        }

}