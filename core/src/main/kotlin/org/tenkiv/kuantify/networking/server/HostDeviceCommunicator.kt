package org.tenkiv.kuantify.networking.server

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.gate.control.output.*
import org.tenkiv.kuantify.hardware.definitions.channel.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.physikal.core.*
import tec.units.indriya.*
import javax.measure.quantity.*
import kotlin.coroutines.*

// Constructor must only be called from Daqc dispatcher.
open class HostDeviceCommunicator(
    scope: CoroutineScope,
    val device: LocalDevice,
    /**
     * Map of path from this communicators device to a [Channel] to transmit additional data not built in to Kuantify.
     * Serialization needs to be handled separately, data sent on this channel must already be serialized.
     */
    private val additionalDataChannels: Map<List<String>, Channel<String>>? = null
) : CoroutineScope {

    private val job = Job(scope.coroutineContext[Job])

    private val deviceId get() = device.uid
    private val deviceRoute = listOf(Route.DEVICE, deviceId)
    private val ioStrandRoute = run {
        val list = ArrayList<String>()
        list += deviceRoute
        list += Route.DAQC_GATE
        list
    }

    override val coroutineContext: CoroutineContext = scope.coroutineContext + job

    init {
        launch {
            device.daqcGateMap.forEach { id, strand ->
                launch {
                    strand.updateBroadcaster.consumeEach {
                        when (val value = it.value) {
                            is DaqcQuantity<*> -> value // send as ComparableQuantity
                            is BinaryState -> value
                        }

                    }
                }
            }
        }

    }

    //TODO: Check additional data channels if cast fails
    //TODO: Throw specific exceptions for errors in message reception
    internal suspend fun receiveMessage(route: List<String>, message: String) {
        when {
            route.first() == Route.DAQC_GATE -> receiveDaqcGateMsg(route.drop(1), message)
            else -> receiveOtherMessage(route, message)
        }
    }

    private suspend fun receiveDaqcGateMsg(route: List<String>, message: String) {
        val gateId = route.first()
        val command = route.drop(1).first()

        when (command) {
            Route.BUFFER -> receiveBufferMsg(gateId, message)
            Route.MAX_ACCEPTABLE_ERROR -> receiveMaxErrorMsg(gateId, message)
            Route.MAX_ELECTRIC_POTENTIAL -> receiveMaxElectricPotential(gateId, message)
            Route.START_SAMPLING -> receiveStartSampling(gateId)
            Route.START_SAMPLING_BINARY_STATE -> receiveStartSamplingBinaryState(gateId)
            Route.START_SAMPLING_PWM -> receiveStartSamplingPwm(gateId)
            Route.START_SAMPLING_TRANSITION_FREQUENCY -> receiveStartSamplingTransitionFrequency(gateId)
            Route.STOP_TRANSCEIVING -> receiveStopTransceiving(gateId)
            Route.PULSE_WIDTH_MODULATE -> receivePulseWidthModulate(gateId, message)
            Route.SUSTAIN_TRANSITION_FREQUENCY -> receiveSustainTransitionFrequency(gateId, message)
            Route.AVG_FREQUENCY -> receiveAvgFrequency(gateId, message)
            else -> receiveOtherDaqcGateMessage(route, message)
        }
    }

    private fun receiveBufferMsg(gateId: String, message: String) {
        val buffer: Boolean = JSON.parse(BooleanSerializer, message)

        (device.daqcGateMap[gateId] as AnalogInput).buffer.set(buffer)
    }

    private fun receiveMaxErrorMsg(gateId: String, message: String) {
        val maxAcceptableError: ComparableQuantity<ElectricPotential> =
            JSON.parse(ComparableQuantitySerializer, message).asType()

        (device.daqcGateMap[gateId] as AnalogInput).maxAcceptableError.set(maxAcceptableError)
    }

    private fun receiveMaxElectricPotential(gateId: String, message: String) {
        val maxElectricPotential: ComparableQuantity<ElectricPotential> =
            JSON.parse(ComparableQuantitySerializer, message).asType()

        (device.daqcGateMap[gateId] as AnalogInput).maxElectricPotential.set(maxElectricPotential)
    }

    private fun receiveStartSampling(gateId: String) {
        (device.daqcGateMap[gateId] as Input<*>).startSampling()
    }

    private fun receiveStartSamplingBinaryState(gateId: String) {
        (device.daqcGateMap[gateId] as DigitalInput).startSamplingBinaryState()
    }

    private fun receiveStartSamplingPwm(gateId: String) {
        (device.daqcGateMap[gateId] as DigitalInput).startSamplingPwm()
    }

    private fun receiveStartSamplingTransitionFrequency(gateId: String) {
        (device.daqcGateMap[gateId] as DigitalInput).startSamplingTransitionFrequency()
    }

    private fun receiveStopTransceiving(gateId: String) {
        device.daqcGateMap[gateId]?.stopTransceiving()
    }

    private fun receivePulseWidthModulate(gateId: String, message: String) {
        val percent: ComparableQuantity<Dimensionless> = JSON.parse(ComparableQuantitySerializer, message).asType()

        (device.daqcGateMap[gateId] as DigitalOutput).pulseWidthModulate(percent, Output.DEFAULT_PANIC_ON_FAILURE)
    }

    private fun receiveSustainTransitionFrequency(gateId: String, message: String) {
        val transitionFrequency: ComparableQuantity<Frequency> =
            JSON.parse(ComparableQuantitySerializer, message).asType()

        (device.daqcGateMap[gateId] as DigitalOutput).sustainTransitionFrequency(
            transitionFrequency,
            Output.DEFAULT_PANIC_ON_FAILURE
        )
    }

    private fun receiveAvgFrequency(gateId: String, message: String) {
        val avgFrequency: ComparableQuantity<Frequency> =
            JSON.parse(ComparableQuantitySerializer, message).asType()

        (device.daqcGateMap[gateId] as DigitalChannel<*>).avgFrequency.set(avgFrequency)
    }

    private suspend fun receiveOtherMessage(route: List<String>, message: String) {
        if (additionalDataChannels == null) {
            //TODO: Handle invalid message
        } else {
            additionalDataChannels[route]?.send(message) ?: TODO("Handle invalid message")
        }
    }

    @Suppress("NAME_SHADOWING")
    private suspend fun receiveOtherDaqcGateMessage(route: List<String>, message: String) {
        if (additionalDataChannels == null) {
            //TODO: Handle invalid message
        } else {
            val route = mutableListOf(Route.DAQC_GATE).apply { addAll(route) }
            additionalDataChannels[route]?.send(message) ?: TODO("Handle invalid message")
        }
    }

    fun cancel() {

        job.cancel()
    }

}