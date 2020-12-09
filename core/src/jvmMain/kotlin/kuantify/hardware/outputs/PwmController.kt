/*
 * Copyright 2020 Tenkiv, Inc.
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

package kuantify.hardware.outputs

import kotlinx.coroutines.flow.*
import kuantify.data.*
import kuantify.gate.control.*
import kuantify.gate.control.output.*
import kuantify.hardware.channel.*
import kuantify.lib.*
import kuantify.trackable.*
import physikal.*
import physikal.types.*

/**
 * Abstract class for a controller which outputs a percent to a single digital output channel from a [Quantity].
 *
 * @param digitalOutput The digital output
 */
public abstract class PwmController<QT : Quantity<QT>>(
    public val digitalOutput: DigitalOutput
) : ProcessedControlGate<DaqcQuantity<QT>, DaqcQuantity<Dimensionless>>(), QuantityOutput<QT> {
    protected final override val parentGate: DigitalOutput
        get() = digitalOutput

    override val parentValueFlow: Flow<ValueInstant<DaqcQuantity<Dimensionless>>>
        get() = digitalOutput.pwmFlow

    public val avgPeriod: UpdatableQuantity<Time>
        get() = digitalOutput.avgPeriod

    public final override val isTransceiving: Trackable<Boolean>
        get() = digitalOutput.isTransceivingFrequency

    init {
        initCoroutines()
    }

    protected final override suspend fun setParentOutput(setting: DaqcQuantity<Dimensionless>): SettingViability =
        digitalOutput.pulseWidthModulateIV(setting)

}