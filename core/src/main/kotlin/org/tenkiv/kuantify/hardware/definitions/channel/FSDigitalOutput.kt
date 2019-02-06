package org.tenkiv.kuantify.hardware.definitions.channel

import kotlinx.coroutines.channels.*
import kotlinx.serialization.json.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.gate.control.output.*
import org.tenkiv.kuantify.hardware.outputs.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.*
import org.tenkiv.kuantify.networking.configuration.*
import tec.units.indriya.*
import javax.measure.quantity.*

@Suppress("LeakingThis")
abstract class LocalDigitalOutput : DigitalOutput, NetworkConfiguredSide, NetworkConfiguredCombined {

    abstract val uid: String

    private val thisAsBinaryStateController = SimpleBinaryStateController(this)

    private val thisAsPwmController = SimplePwmController(this)

    private val thisAsFrequencyController = SimpleFrequencyController(this)

    private val _isTransceiving: InitializedUpdatable<Boolean> = Updatable(false)
    override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    private val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<DigitalChannelValue>>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<DigitalChannelValue>>
        get() = _updateBroadcaster

    init {
        onAnyTransceivingChange {
            _isTransceiving.value = it
        }
    }

    override fun asBinaryStateController(): BinaryStateOutput = thisAsBinaryStateController

    override fun asPwmController(avgFrequency: ComparableQuantity<Frequency>): QuantityOutput<Dimensionless> {
        this.avgFrequency.set(avgFrequency)
        return thisAsPwmController
    }

    override fun asFrequencyController(avgFrequency: ComparableQuantity<Frequency>): QuantityOutput<Frequency> {
        this.avgFrequency.set(avgFrequency)
        return thisAsFrequencyController
    }

    override fun combinedConfig(config: CombinedRouteConfig) {
        config.add {
            digitalChannelRouting(this@LocalDigitalOutput, uid)
        }
    }

    override fun sideConfig(config: SideRouteConfig) {
        config.add {
            digitalChannelIsTransceivingLocal(isTransceivingBinaryState, uid, RC.IS_TRANSCEIVING_BIN_STATE)
            digitalChannelIsTransceivingLocal(isTransceivingPwm, uid, RC.IS_TRANSCEIVING_PWM)
            digitalChannelIsTransceivingLocal(isTransceivingFrequency, uid, RC.IS_TRANSCEIVING_FREQUENCY)

            route(RC.DAQC_GATE, uid, RC.VALUE) to handler<ValueInstant<DigitalChannelValue>>(
                isFullyBiDirectional = true
            ) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(DigitalChannelValue.serializer()), it)
                }

                receiveMessage(NullResolutionStrategy.PANIC) {
                    val setting = Json.parse(ValueInstantSerializer(DigitalChannelValue.serializer()), it)
                    val (value, _) = setting
                    when (value) {
                        is DigitalChannelValue.BinaryState -> setOutputState(value.state)
                        is DigitalChannelValue.Percentage -> pulseWidthModulate(value.percent)
                        is DigitalChannelValue.Frequency -> sustainTransitionFrequency(value.frequency)
                    }
                    _updateBroadcaster.send(setting)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }
            }
        }
    }
}

@Suppress("LeakingThis")
abstract class FSRemoteDigitalOutput : DigitalOutput, NetworkConfiguredSide, NetworkConfiguredCombined {

    abstract val uid: String

    private val thisAsBinaryStateController = SimpleBinaryStateController(this)

    private val thisAsPwmController = SimplePwmController(this)

    private val thisAsFrequencyController = SimpleFrequencyController(this)

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

        onAnyTransceivingChange {
            _isTransceiving.value = it
        }
    }

    override fun asBinaryStateController(): BinaryStateOutput = thisAsBinaryStateController

    override fun asPwmController(avgFrequency: ComparableQuantity<Frequency>): QuantityOutput<Dimensionless> {
        this.avgFrequency.set(avgFrequency)
        return thisAsPwmController
    }

    override fun asFrequencyController(avgFrequency: ComparableQuantity<Frequency>): QuantityOutput<Frequency> {
        this.avgFrequency.set(avgFrequency)
        return thisAsFrequencyController
    }

    override fun combinedConfig(config: CombinedRouteConfig) {
        config.add {
            digitalChannelRouting(this@FSRemoteDigitalOutput, uid)
        }
    }

    override fun sideConfig(config: SideRouteConfig) {
        config.add {
            digitalChannelIsTransceivingRemote(_isTransceivingBinaryState, uid, RC.IS_TRANSCEIVING_BIN_STATE)
            digitalChannelIsTransceivingRemote(_isTransceivingPwm, uid, RC.IS_TRANSCEIVING_PWM)
            digitalChannelIsTransceivingRemote(_isTransceivingFrequency, uid, RC.IS_TRANSCEIVING_FREQUENCY)

            route(RC.DAQC_GATE, uid, RC.VALUE) to handler<ValueInstant<DigitalChannelValue>>(
                isFullyBiDirectional = true
            ) {
                serializeMessage {
                    Json.stringify(ValueInstantSerializer(DigitalChannelValue.serializer()), it)
                }

                receiveMessage(NullResolutionStrategy.PANIC) {
                    val setting = Json.parse(ValueInstantSerializer(DigitalChannelValue.serializer()), it)
                    val (value, instant) = setting
                    when (value) {
                        is DigitalChannelValue.BinaryState -> _binaryStateBroadcaster.send(value.state at instant)
                        is DigitalChannelValue.Percentage -> _pwmBroadcaster.send(value.percent at instant)
                        is DigitalChannelValue.Frequency ->
                            _transitionFrequencyBroadcaster.send(value.frequency at instant)
                    }
                    _updateBroadcaster.send(setting)
                }

                setLocalUpdateChannel(updateBroadcaster.openSubscription()) withUpdateChannel {
                    send()
                }
            }
        }
    }
}