/*
 * Copyright 2018 Tenkiv, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.tenkiv.daqc

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.coral.at
import org.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.hardware.definitions.device.Device
import org.tenkiv.physikal.core.hertz
import org.tenkiv.physikal.core.percent
import tec.units.indriya.ComparableQuantity
import tec.units.indriya.unit.MetricPrefix.*
import tec.units.indriya.unit.Units.*
import java.time.Instant
import java.util.*
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Frequency


class DigitalGibberingSensor : Input<BinaryState> {
    override val updateRate: ComparableQuantity<Frequency>
        get() = throw Exception("Empty Test Class Exception")
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = throw Exception("Empty Test Class Exception")
    override val broadcastChannel = ConflatedBroadcastChannel<BinaryStateMeasurement>()
    override val isActive: Boolean = false

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                broadcastChannel.offer(BinaryState.On.at(Instant.now()))
            }
        }, 100, 100)
    }

    override fun activate() {}

    override fun deactivate() {}

    fun cancel() {
        timer.cancel()
        broadcastChannel.close()
    }
}

class DigitalInputGibberingSensor : DigitalInput() {
    override val updateRate: ComparableQuantity<Frequency>
        get() = throw Exception("Empty Test Class Exception")

    override fun activateForTransitionFrequency(avgFrequency: DaqcQuantity<Frequency>) {
        throw Exception("Empty Test Class Exception")
    }

    override fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>) {
        throw Exception("Empty Test Class Exception")
    }

    override val isActiveForBinaryState: Boolean = false
    override val isActiveForPwm: Boolean = false
    override val isActiveForTransitionFrequency: Boolean = false
    override val pwmIsSimulated: Boolean = false
    override val transitionFrequencyIsSimulated: Boolean = false
    override val device: Device
        get() = throw Exception("Empty Test Class Exception")
    override val hardwareNumber: Int
        get() = throw Exception("Empty Test Class Exception")
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = throw Exception("Empty Test Class Exception")

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                _binaryStateBroadcastChannel.offer(BinaryState.On.at(Instant.now()))
                _transitionFrequencyBroadcastChannel.offer(DaqcQuantity.of(10.hertz).at(Instant.now()))
                _pwmBroadcastChannel.offer(DaqcQuantity.of(10.percent).at(Instant.now()))
            }
        }, 100, 100)
    }

    override fun activate() {}

    override fun deactivate() {}

    fun cancel() {
        timer.cancel()
        broadcastChannel.close()
    }

}

class AnalogGibberingSensor : Input<DaqcQuantity<ElectricPotential>> {
    override val updateRate: ComparableQuantity<Frequency>
        get() = throw Exception("Empty Test Class Exception")
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = throw Exception("Empty Test Class Exception")
    override val broadcastChannel = ConflatedBroadcastChannel<ValueInstant<DaqcQuantity<ElectricPotential>>>()

    override val isActive: Boolean = false

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                broadcastChannel.offer(
                    DaqcQuantity.of(random.nextInt(5000), MILLI(VOLT)).at(Instant.now())
                )

            }
        }, 100, 100)
    }

    override fun activate() {}

    override fun deactivate() {}

    fun cancel() {
        timer.cancel()
        broadcastChannel.close()
    }
}

class AnalogInputGibberingSensor : AnalogInput() {
    override var buffer: Boolean = false
    override val updateRate: ComparableQuantity<Frequency>
        get() = throw Exception("Empty Test Class Exception")
    override var maxAcceptableError: ComparableQuantity<ElectricPotential>
        get() = throw Exception("Empty Test Class Exception")
        set(value) {}
    override var maxElectricPotential: ComparableQuantity<ElectricPotential>
        get() = throw Exception("Empty Test Class Exception")
        set(value) {}
    override val device: Device
        get() = throw Exception("Empty Test Class Exception")
    override val hardwareNumber: Int
        get() = throw Exception("Empty Test Class Exception")
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = throw Exception("Empty Test Class Exception")
    override val broadcastChannel = ConflatedBroadcastChannel<ValueInstant<DaqcQuantity<ElectricPotential>>>()

    override val isActive: Boolean = false

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                broadcastChannel.offer(
                        DaqcQuantity.of(
                                random.nextInt(5000),
                            MILLI(VOLT)
                        ).at(Instant.now())
                )
            }
        }, 100, 100)
    }

    override fun activate() {}

    override fun deactivate() {}

    fun cancel() {
        timer.cancel()
        broadcastChannel.close()
    }
}