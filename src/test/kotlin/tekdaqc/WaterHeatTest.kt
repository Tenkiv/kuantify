package tekdaqc

import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.LineNoiseFrequency
import org.tenkiv.daqc.hardware.inputs.thermocouples.ThermocoupleK
import org.tenkiv.daqc.monitoring.BinaryNNPIDController
import org.tenkiv.daqc.networking.Locator
import org.tenkiv.daqc.networking.NetworkProtocol
import org.tenkiv.physikal.core.celsius
import org.tenkiv.physikal.core.hertz
import org.tenkiv.tekdaqc.TekdaqcDevice
import org.tenkiv.tekdaqc.TekdaqcLocator

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
class WaterHeatTest : StringSpec() {
    init {
        "Heating Water Test"{

            val tekdaqcLoc = TekdaqcLocator()

            tekdaqcLoc.search()

            Locator.addDeviceLocator(tekdaqcLoc)

            println("Pre Tekdaqc Discovery")

            launch(CommonPool) {
                tekdaqcLoc.openNewCoroutineListener(CommonPool) {

                    if (it.serialNumber == "00000000000000000000000000000012") {
                        println("Tekdaqc Discovered")

                        val tekdaqc = it.wrappedDevice as TekdaqcDevice

                        tekdaqc.connect(LineNoiseFrequency.AccountFor(60.hertz), NetworkProtocol.TELNET)
                        val thermo = ThermocoupleK(tekdaqc.analogInputs[0], 50.celsius)

                        //tekdaqc.wrappedTekdaqc.readAnalogInput(36,5)

                        thermo.failureBroadcastChannel.consumeEach { it.value.printStackTrace() }

                        thermo.activate()

                        //tekdaqc.wrappedTekdaqc.sample(0)

                        val controllrer = BinaryNNPIDController(thermo, tekdaqc.digitalOutputs[0], DaqcQuantity(42.celsius))

                    }
                }
                Thread.sleep(600000)
            }
        }
    }
}