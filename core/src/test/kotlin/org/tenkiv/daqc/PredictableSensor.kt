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
import org.tenkiv.physikal.core.volt
import tec.units.indriya.ComparableQuantity
import java.time.Instant
import java.util.*
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Frequency

class PredictableAnalogSensor : Input<DaqcQuantity<ElectricPotential>> {
    override val sampleRate: ComparableQuantity<Frequency>
        get() = throw Exception("Empty Test Class Exception")
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = throw Exception("Empty Test Class Exception")
    override val isActive: Boolean = false
    override val broadcastChannel = ConflatedBroadcastChannel<ValueInstant<DaqcQuantity<ElectricPotential>>>()

    override fun activate() {}

    override fun deactivate() {}

    var iteration = 0

    var sendingOrder = arrayListOf(
            DaqcQuantity.of(2.volt),
            DaqcQuantity.of(4.volt),
            DaqcQuantity.of(6.volt),
            DaqcQuantity.of(8.volt),
            DaqcQuantity.of(10.volt),
            DaqcQuantity.of(12.volt),
            DaqcQuantity.of(14.volt),
            DaqcQuantity.of(16.volt),
            DaqcQuantity.of(18.volt),
            DaqcQuantity.of(20.volt))

    init {
        val timer = Timer(false)
        // Just a very hacky way of simulating an input. Needs to be thread safe to be predictable.
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (iteration < sendingOrder.size) {
                    broadcastChannel.offer(sendingOrder[iteration].at(Instant.now()))
                    iteration++
                } else {
                    timer.cancel()
                }

            }
        }, 100, 100)
    }
}

class PredictableDigitalSensor : Input<BinaryState> {
    override val sampleRate: ComparableQuantity<Frequency>
        get() = throw Exception("Empty Test Class Exception")
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = throw Exception("Empty Test Class Exception")
    override val broadcastChannel: ConflatedBroadcastChannel<ValueInstant<BinaryState>> = ConflatedBroadcastChannel()
    override val isActive: Boolean = true

    override fun activate() {}

    override fun deactivate() {}
    var iteration = 0

    var sendingOrder = arrayListOf(
            BinaryState.On,
            BinaryState.On,
            BinaryState.On,
            BinaryState.On,
            BinaryState.On,
            BinaryState.On,
            BinaryState.On,
            BinaryState.On,
            BinaryState.Off,
            BinaryState.Off)

    init {
        val timer = Timer(false)
        // Just a very hacky way of simulating an input. Needs to be thread safe to be predictable.
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (iteration < sendingOrder.size) {
                    broadcastChannel.offer(sendingOrder[iteration].at(Instant.now()))
                    iteration++
                } else {
                    timer.cancel()
                }
            }
        }, 100, 100)
    }

}