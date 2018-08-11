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

import io.kotlintest.specs.StringSpec
import org.tenkiv.daqc.hardware.inputs.SimpleBinaryStateSensor
import org.tenkiv.daqc.hardware.inputs.SimpleDigitalFrequencySensor
import org.tenkiv.daqc.hardware.inputs.SimplePwmSensor

class GenericSensorsTest : StringSpec() {
    init {

        "Check Basic Sensors"{

            val sensorStatus = BooleanArray(3, { false })

            val binaryStateSensor = SimpleBinaryStateSensor(DigitalInputGibberingSensor())
            binaryStateSensor.addTrigger({ true }, { sensorStatus[0] = true })

            val digitalFreqStateSensor = SimpleDigitalFrequencySensor(DigitalInputGibberingSensor())
            digitalFreqStateSensor.addTrigger({ true }, { sensorStatus[1] = true })

            val digitalPwmStateSensor = SimplePwmSensor(DigitalInputGibberingSensor())
            digitalPwmStateSensor.addTrigger({ true }, { sensorStatus[2] = true })

            Thread.sleep(2000)

            assert(!sensorStatus.contains(false))

        }
    }
}