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

package org.tenkiv.kuantify

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.acquire.input.*
import org.tenkiv.kuantify.hardware.definitions.channel.*
import org.tenkiv.physikal.core.*
import tec.units.indriya.*
import tec.units.indriya.unit.MetricPrefix.*
import tec.units.indriya.unit.Units.*
import java.time.*
import java.util.*
import javax.measure.quantity.*
import kotlin.coroutines.*


class DigitalGibberingSensor : Input<BinaryState> {
    override val coroutineContext: CoroutineContext = Job()
    override val updateRate: ComparableQuantity<Frequency>
        get() = throw Exception("Empty Test Class Exception")
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = throw Exception("Empty Test Class Exception")
    override val updateBroadcaster = ConflatedBroadcastChannel<BinaryStateMeasurement>()
    override val isTransceiving: Boolean = false

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                updateBroadcaster.offer(BinaryState.On.at(Instant.now()))
            }
        }, 100, 100)
    }

    override fun startSampling() {}

    override fun stopTransceiving() {}

    fun cancel() {
        timer.cancel()
        updateBroadcaster.close()
    }
}

class DigitalInputGibberingSensor : DigitalInput() {
    override val updateRate: ComparableQuantity<Frequency>
        get() = throw Exception("Empty Test Class Exception")

    override fun startSamplingTransitionFrequency(avgFrequency: DaqcQuantity<Frequency>) {
        throw Exception("Empty Test Class Exception")
    }

    override fun startSamplingPwm(avgFrequency: DaqcQuantity<Frequency>) {
        throw Exception("Empty Test Class Exception")
    }

    override val isTransceivingBinaryState: Boolean = false
    override val isTransceivingPwm: Boolean = false
    override val isTransceivingFrequency: Boolean = false
    override val pwmIsSimulated: Boolean = false
    override val transitionFrequencyIsSimulated: Boolean = false
    override val device: DataAcquisitionDevice
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

    override fun startSampling() {}

    override fun stopTransceiving() {}

    fun cancel() {
        timer.cancel()
        updateBroadcaster.close()
    }

}

class AnalogGibberingSensor : Input<DaqcQuantity<ElectricPotential>> {
    override val updateRate: ComparableQuantity<Frequency>
        get() = throw Exception("Empty Test Class Exception")
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = throw Exception("Empty Test Class Exception")
    override val updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<DaqcQuantity<ElectricPotential>>>()
    override val coroutineContext: CoroutineContext = Job()

    override val isTransceiving: Boolean = false

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                updateBroadcaster.offer(
                    DaqcQuantity.of(random.nextInt(5000), MILLI(VOLT)).at(Instant.now())
                )

            }
        }, 100, 100)
    }

    override fun startSampling() {}

    override fun stopTransceiving() {}

    fun cancel() {
        timer.cancel()
        updateBroadcaster.close()
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
    override val device: DataAcquisitionDevice
        get() = throw Exception("Empty Test Class Exception")
    override val hardwareNumber: Int
        get() = throw Exception("Empty Test Class Exception")
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = throw Exception("Empty Test Class Exception")
    override val updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<DaqcQuantity<ElectricPotential>>>()

    override val isTransceiving: Boolean = false

    val random = Random()

    val timer = Timer(false)

    init {
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                updateBroadcaster.offer(
                    DaqcQuantity.of(
                        random.nextInt(5000),
                        MILLI(VOLT)
                    ).at(Instant.now())
                )
            }
        }, 100, 100)
    }

    override fun startSampling() {}

    override fun stopTransceiving() {}

    fun cancel() {
        timer.cancel()
        updateBroadcaster.close()
    }
}