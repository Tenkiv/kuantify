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

package org.tenkiv.daqc.hardware.definitions.device

import org.tenkiv.daqc.data.DaqcValue
import org.tenkiv.daqc.gate.control.output.Output
import org.tenkiv.daqc.hardware.definitions.channel.AnalogOutput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import org.tenkiv.daqc.networking.SharingStatus

/**
 * Interface defining [Device]s which have either [AnalogOutput]s, [DigitalOutput]s, or both.
 */
interface ControlDevice : Device {

    /**
     * List of all [AnalogOutput]s that this [ControlDevice] has.
     */
    val analogOutputs: List<AnalogOutput>

    /**
     * List of all [DigitalOutput]s that this [ControlDevice] has.
     */
    val digitalOutputs: List<DigitalOutput>

    /**
     * If this [ControlDevice] has any [AnalogOutput]s.
     */
    val hasAnalogOutputs: Boolean

    /**
     * If this [ControlDevice] has any [DigitalOutput]s.
     */
    val hasDigitalOutputs: Boolean

    /**
     * A [MutableMap] of all outputs shared for remote access.
     */
    val sharedOutputs: MutableMap<SharingStatus, Output<DaqcValue>>

}