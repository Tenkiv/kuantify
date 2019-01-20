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

package org.tenkiv.kuantify.hardware.definitions.channel

import kotlinx.coroutines.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import kotlin.coroutines.*
import org.tenkiv.kuantify.data.BinaryState as DaqcValueBinaryState
import javax.measure.quantity.Frequency as FrequencyDimension

/**
 * Defines the basic features of a channel owned by a [Device].
 */
interface DaqcChannel<out D : Device> : CoroutineScope {

    /**
     * The [Device] which hosts this channel.
     */
    val device: D

    /**
     * The hardware number of the channel on the [device].
     */
    val hardwareNumber: Int

    override val coroutineContext: CoroutineContext get() = device.coroutineContext
}