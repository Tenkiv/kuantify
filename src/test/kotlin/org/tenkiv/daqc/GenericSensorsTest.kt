package org.tenkiv.daqc

import io.kotlintest.specs.StringSpec
import org.tenkiv.daqc.hardware.inputs.SimpleBinaryStateSensor
import org.tenkiv.daqc.hardware.inputs.SimpleDigitalFrequencySensor
import org.tenkiv.daqc.hardware.inputs.SimplePwmSensor

/**
 * Copyright 2017 TENKIV, INC.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following
disclaimer in the documentation and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote
products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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