package org.tenkiv.kuantify.networking.server

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.json.*
import org.tenkiv.kuantify.data.*
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

    //TODO: Throw specific exceptions for errors in message reception
    internal fun receiveMessage(route: List<String>, message: String) {
        when {
            route.first() == Route.DAQC_GATE -> receiveIOStrandMsg(route.drop(1), message)
        }
    }

    @Suppress("NAME_SHADOWING")
    private fun receiveIOStrandMsg(route: List<String>, message: String) {
        val strandId = route.first()
        val route = route.drop(1).first()

        when (route) {
            Route.BUFFER -> receiveBufferMsg(strandId, message)
            Route.MAX_ACCEPTABLE_ERROR -> receiveMaxErrorMsg(strandId, message)
            Route.MAX_ELECTRIC_POTENTIAL -> receiveMaxElectricPotential(strandId, message)
            Route.START_SAMPLING -> receiveStartSampling(strandId)
            Route.START_SAMPLING_PWM -> receiveStartSamplingPwm(strandId)
            Route.START_SAMPLING_TRANSITION_FREQUENCY -> receiveStartSamplingTransitionFrequency(strandId)
            Route.STOP_TRANSCEIVING -> receiveStopTransceiving(strandId)
            Route.PULSE_WIDTH_MODULATE -> receivePulseWidthModulate(strandId, message)
            Route.SUSTAIN_TRANSITION_FREQUENCY -> receiveSustainTransitionFrequency(strandId, message)
        }
    }

    private fun receiveBufferMsg(strandId: String, message: String) {
        val buffer: Boolean = JSON.parse(BooleanSerializer, message)

        (device.daqcGateMap[strandId] as AnalogInput)
    }

    private fun receiveMaxErrorMsg(strandId: String, message: String) {
        val maxAcceptableError: ComparableQuantity<ElectricPotential> =
            JSON.parse(ComparableQuantitySerializer, message).asType()

    }

    private fun receiveMaxElectricPotential(strandId: String, message: String) {
        val maxElectricPotential: ComparableQuantity<ElectricPotential> =
            JSON.parse(ComparableQuantitySerializer, message).asType()

    }

    private fun receiveStartSampling(strandId: String) {


    }

    private fun receiveStartSamplingPwm(strandId: String) {

    }

    private fun receiveStartSamplingTransitionFrequency(strandId: String) {

    }

    private fun receiveStopTransceiving(strandId: String) {

    }

    private fun receivePulseWidthModulate(strandId: String, message: String) {
        val percent: ComparableQuantity<Dimensionless> = JSON.parse(ComparableQuantitySerializer, message).asType()

    }

    private fun receiveSustainTransitionFrequency(strandId: String, message: String) {
        val transitionFrequency: ComparableQuantity<Frequency> =
            JSON.parse(ComparableQuantitySerializer, message).asType()

    }

    fun cancel() {

        job.cancel()
    }

}