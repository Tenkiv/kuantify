package org.tenkiv.kuantify.hardware.definitions.channel

import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.gate.control.output.*
import org.tenkiv.kuantify.hardware.outputs.*
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
        }
    }
}