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

package org.tenkiv.daqc.hardware.definitions.channel

import org.tenkiv.daqc.QuantityInput
import tec.units.indriya.ComparableQuantity
import javax.measure.quantity.ElectricPotential

/**
 * Class defining the basic features of an input which reads analog signals.
 */
abstract class AnalogInput : QuantityInput<ElectricPotential>, DaqcChannel {

    /**
     * Denotes if the analog input's buffer is activated.
     *
     * Implementing backing  field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    abstract var buffer: Boolean

    /**
     * Denotes the maximum acceptable error for the [AnalogInput].
     *
     * Implementing backing field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    abstract var maxAcceptableError: ComparableQuantity<ElectricPotential>

    /**
     * Denotes the maximum [ElectricPotential] that the [AnalogInput] can read.
     *
     * Implementing backing field must be marked with [Volatile] annotation or otherwise provide safety for
     * reads from multiple threads.
     */
    abstract var maxElectricPotential: ComparableQuantity<ElectricPotential>

}