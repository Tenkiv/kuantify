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
import org.tenkiv.daqc.gate.acquire.input.Input
import org.tenkiv.daqc.hardware.definitions.channel.AnalogInput
import org.tenkiv.daqc.hardware.definitions.channel.DigitalInput
import org.tenkiv.daqc.networking.SharingStatus

/**
 * Interface defining [Device]s which have either [AnalogInput]s, [DigitalInput]s, or both.
 */
interface DataAcquisitionDevice : Device {

    /**
     * List of all [AnalogInput]s that this [DataAcquisitionDevice] has.
     */
    val analogInputs: List<AnalogInput>

    /**
     * List of all [DigitalInput]s that this [DataAcquisitionDevice] has.
     */
    val digitalInputs: List<DigitalInput>

    /**
     * If this [DataAcquisitionDevice] has any [AnalogInputs]s.
     */
    val hasAnalogInputs: Boolean

    /**
     * If this [DataAcquisitionDevice] has any [DigitalInputs]s.
     */
    val hasDigitalInputs: Boolean

    /**
     * A [MutableMap] of all inputs shared for remote access.
     */
    val sharedInputs: MutableMap<SharingStatus, Input<DaqcValue>>

}