package org.tenkiv.kuantify.hardware.definitions.channel

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

    override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<DigitalChannelValue>>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val failureBroadcaster: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val isTransceivingBinaryState: InitializedTrackable<Boolean>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val isTransceivingPwm: InitializedTrackable<Boolean>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val isTransceivingFrequency: InitializedTrackable<Boolean>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val avgFrequency: UpdatableQuantity<Frequency>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override val binaryStateBroadcaster: ConflatedBroadcastChannel<out BinaryStateMeasurement>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val pwmBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Dimensionless>>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val transitionFrequencyBroadcaster: ConflatedBroadcastChannel<out QuantityMeasurement<Frequency>>
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun startSamplingTransitionFrequency() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun startSamplingPwm() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun startSamplingBinaryState() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stopTransceiving() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun combinedConfig(config: CombinedRouteConfig) {
        config.add {
            digitalChannelRouting(this@FSRemoteDigitalInput, uid)
        }
    }

    override fun sideConfig(config: SideRouteConfig) {
        config.add {
            digitalChannelIsTransceivingRemote(isTransceivingBinaryState, uid, RC.IS_TRANSCEIVING_BIN_STATE)
            digitalChannelIsTransceivingRemote(isTransceivingPwm, uid, RC.IS_TRANSCEIVING_PWM)
            digitalChannelIsTransceivingRemote(isTransceivingFrequency, uid, RC.IS_TRANSCEIVING_FREQUENCY)


        }
    }
}