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

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.ValueInstant
import org.tenkiv.kuantify.data.DaqcQuantity
import org.tenkiv.kuantify.hardware.definitions.channel.AnalogInput
import org.tenkiv.kuantify.hardware.definitions.channel.DigitalInput
import org.tenkiv.kuantify.hardware.definitions.device.DataAcquisitionDevice
import tec.units.indriya.ComparableQuantity
import javax.measure.quantity.ElectricPotential
import javax.measure.quantity.Frequency

class EmptyAnalogInput : AnalogInput() {
    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = throw Exception("Empty Test Class Exception")
    override val updateRate: ComparableQuantity<Frequency>
        get() = throw Exception("Empty Test Class Exception")
    override val device: DataAcquisitionDevice
        get() = throw Exception("Empty Test Class Exception")
    override val hardwareNumber: Int
        get() = throw Exception("Empty Test Class Exception")
    override val isActive: Boolean
        get() = throw Exception("Empty Test Class Exception")

    override fun activate() {}

    override val broadcastChannel:
            ConflatedBroadcastChannel<QuantityMeasurement<ElectricPotential>> = ConflatedBroadcastChannel()

    override fun deactivate() {}

    override var buffer: Boolean
        get() = throw Exception("Empty Test Class Exception")
        set(value) {}
    override var maxAcceptableError: ComparableQuantity<ElectricPotential>
        get() = throw Exception("Empty Test Class Exception")
        set(value) {}
    override var maxElectricPotential: ComparableQuantity<ElectricPotential>
        get() = throw Exception("Empty Test Class Exception")
        set(value) {}
}

class EmptyDigitalInput : DigitalInput() {
    override val updateRate: ComparableQuantity<Frequency>
        get() = throw Exception("Empty Test Class Exception")

    override fun activateForTransitionFrequency(avgFrequency: DaqcQuantity<Frequency>) {
        throw Exception("Empty Test Class Exception")
    }

    override val failureBroadcastChannel: ConflatedBroadcastChannel<out ValueInstant<Throwable>>
        get() = throw Exception("Empty Test Class Exception")

    override val transitionFrequencyIsSimulated: Boolean
        get() = throw Exception("Empty Test Class Exception")
    override val pwmIsSimulated: Boolean
        get() = throw Exception("Empty Test Class Exception")

    override val isActiveForBinaryState: Boolean
        get() = throw Exception("Empty Test Class Exception")
    override val isActiveForPwm: Boolean
        get() = throw Exception("Empty Test Class Exception")
    override val isActiveForTransitionFrequency: Boolean
        get() = throw Exception("Empty Test Class Exception")

    override fun activateForPwm(avgFrequency: DaqcQuantity<Frequency>) {
        throw Exception("Empty Test Class Exception")
    }

    override val device: DataAcquisitionDevice get() = throw Exception("Empty Test Class Exception")

    override val hardwareNumber: Int get() = throw Exception("Empty Test Class Exception")

    override fun activate() {}

    override fun deactivate() {}

}