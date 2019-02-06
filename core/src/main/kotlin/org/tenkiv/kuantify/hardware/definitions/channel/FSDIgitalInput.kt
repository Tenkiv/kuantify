package org.tenkiv.kuantify.hardware.definitions.channel

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.serialization.json.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.configuration.*
import javax.measure.quantity.*

private inline fun SideRouteConfig.startSamplingLocal(uid: String, rc: String, crossinline start: () -> Unit) {
    route(RC.DAQC_GATE, uid, rc) to handler<Ping>(isFullyBiDirectional = false) {
        receive {
            start()
        }
    }
}

private fun SideRouteConfig.startSamplingRemote(uid: String, rc: String, channel: ReceiveChannel<Ping>) {
    route(RC.DAQC_GATE, uid, rc) to handler<Ping>(isFullyBiDirectional = false) {
        setLocalUpdateChannel(channel) withUpdateChannel {
            send()
        }
    }
}

interface LocalDigitalInput : DigitalInput, NetworkConfiguredSide, NetworkConfiguredCombined {

    val uid: String

    override fun combinedConfig(config: CombinedRouteConfig) {
        config.add {
            digitalChannelRouting(this@LocalDigitalInput, uid)
        }
    }

    override fun sideConfig(config: SideRouteConfig) {
        config.add {
            digitalChannelIsTransceivingLocal(isTransceivingBinaryState, uid, RC.IS_TRANSCEIVING_BIN_STATE)
            digitalChannelIsTransceivingLocal(isTransceivingPwm, uid, RC.IS_TRANSCEIVING_PWM)
            digitalChannelIsTransceivingLocal(isTransceivingFrequency, uid, RC.IS_TRANSCEIVING_FREQUENCY)

            startSamplingLocal(uid, RC.START_SAMPLING_BINARY_STATE, ::startSamplingBinaryState)
            startSamplingLocal(uid, RC.START_SAMPLING_PWM, ::startSamplingPwm)
            startSamplingLocal(uid, RC.START_SAMPLING_TRANSITION_FREQUENCY, ::startSamplingTransitionFrequency)

            route(RC.DAQC_GATE, uid, RC.VALUE) to handler<ValueInstant<DigitalChannelValue>>(
                isFullyBiDirectional = false
            ) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(DigitalChannelValue.serializer()), it)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }
            }
        }
    }
}

abstract class FSRemoteDigitalInput : DigitalInput, NetworkConfiguredSide, NetworkConfiguredCombined {
    abstract val uid: String

    private val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<DigitalChannelValue>>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<DigitalChannelValue>>
        get() = _updateBroadcaster

    private val _binaryStateBroadcaster = ConflatedBroadcastChannel<BinaryStateMeasurement>()
    override val binaryStateBroadcaster: ConflatedBroadcastChannel<out BinaryStateMeasurement>
        get() = _binaryStateBroadcaster

    private val _pwmBroadcaster = ConflatedBroadcastChannel<QuantityMeasurement<Dimensionless>>()
    override val pwmBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Dimensionless>>
        get() = _pwmBroadcaster

    private val _transitionFrequencyBroadcaster = ConflatedBroadcastChannel<QuantityMeasurement<Frequency>>()
    override val transitionFrequencyBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Frequency>>
        get() = _transitionFrequencyBroadcaster

    private val _failureBroadcaster = ConflatedBroadcastChannel<ValueInstant<Throwable>>()
    override val failureBroadcaster: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = _failureBroadcaster

    private val _isTransceiving = Updatable(false)
    override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    private val _isTransceivingBinaryState = Updatable(false)
    override val isTransceivingBinaryState: InitializedTrackable<Boolean>
        get() = _isTransceivingBinaryState

    private val _isTransceivingPwm = Updatable(false)
    override val isTransceivingPwm: InitializedTrackable<Boolean>
        get() = _isTransceivingPwm

    private val _isTransceivingFrequency = Updatable(false)
    override val isTransceivingFrequency: InitializedTrackable<Boolean>
        get() = _isTransceivingFrequency

    init {
        launch {
            updateBroadcaster.consumeEach { (value, instant) ->
                when (value) {
                    is DigitalChannelValue.BinaryState -> _binaryStateBroadcaster.send(value.state at instant)
                    is DigitalChannelValue.Percentage -> _pwmBroadcaster.send(value.percent at instant)
                    is DigitalChannelValue.Frequency -> _transitionFrequencyBroadcaster.send(value.frequency at instant)
                }
            }
        }

        onAnyTransceivingChange {
            _isTransceiving.value = it
        }
    }

    override val avgFrequency: UpdatableQuantity<Frequency> = Updatable()

    private val startSamplingTransitionFrequencyChannel = Channel<Ping>(Channel.CONFLATED)
    override fun startSamplingTransitionFrequency() {
        startSamplingTransitionFrequencyChannel.offer(null)
    }

    private val startSamplingPwmChannel = Channel<Ping>(Channel.CONFLATED)
    override fun startSamplingPwm() {
        startSamplingPwmChannel.offer(null)
    }

    private val startSamplingBinaryStateChannel = Channel<Ping>(Channel.CONFLATED)
    override fun startSamplingBinaryState() {
        startSamplingBinaryStateChannel.offer(null)
    }

    private val stopTransceivingChannel = Channel<Ping>(Channel.CONFLATED)
    override fun stopTransceiving() {
        stopTransceivingChannel.offer(null)
    }

    override fun combinedConfig(config: CombinedRouteConfig) {
        config.add {
            digitalChannelRouting(this@FSRemoteDigitalInput, uid)
        }
    }

    override fun sideConfig(config: SideRouteConfig) {
        config.add {
            digitalChannelIsTransceivingRemote(_isTransceivingBinaryState, uid, RC.IS_TRANSCEIVING_BIN_STATE)
            digitalChannelIsTransceivingRemote(_isTransceivingPwm, uid, RC.IS_TRANSCEIVING_PWM)
            digitalChannelIsTransceivingRemote(_isTransceivingFrequency, uid, RC.IS_TRANSCEIVING_FREQUENCY)

            startSamplingRemote(uid, RC.START_SAMPLING_TRANSITION_FREQUENCY, startSamplingTransitionFrequencyChannel)
            startSamplingRemote(uid, RC.START_SAMPLING_PWM, startSamplingPwmChannel)
            startSamplingRemote(uid, RC.START_SAMPLING_BINARY_STATE, startSamplingBinaryStateChannel)

            route(RC.DAQC_GATE, uid, RC.VALUE) to handler<ValueInstant<DigitalChannelValue>>(
                isFullyBiDirectional = false
            ) {
                receiveMessage(NullResolutionStrategy.PANIC) {
                    val measurement = Json.parse(ValueInstantSerializer(DigitalChannelValue.serializer()), it)
                    _updateBroadcaster.send(measurement)
                }
            }
        }
    }
}