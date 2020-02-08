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

package org.tenkiv.kuantify.hardware.inputs.thermocouples

import arrow.core.*
import kotlinx.coroutines.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.hardware.channel.*
import org.tenkiv.kuantify.hardware.inputs.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.lib.physikal.*
import physikal.*
import physikal.types.*
import kotlin.coroutines.*
import kotlin.math.*

/**
 * A common sensor which measures [Temperature] from [ElectricPotential].
 */
public class ThermocoupleK internal constructor(
    scope: CoroutineScope,
    channel: AnalogInput<*>,
    acceptableError: Quantity<Temperature> = 1.0.degreesCelsius
) : ScAnalogSensor<Temperature>(
    channel,
    maximumEp = 55.0.millivolts,
    acceptableError = 18.0.microvolts * acceptableError.toDoubleIn(Kelvin)
) {
    private val job = Job(scope.coroutineContext[Job])

    public override val coroutineContext: CoroutineContext = scope.coroutineContext + job

    private val noTempRefValueMsg =
        "The temperature reference for device:\n" +
                " ${analogInput.device} \n" +
                "has not yet measured a temperature, thermocouples from this device cannot function until it does."

    protected override fun convertInput(voltage: Quantity<Voltage>): Try<DaqcQuantity<Temperature>> {
        val mv = voltage toDoubleIn Millivolt
        val temperatureReferenceValue = analogInput.device.temperatureReference.updateBroadcaster.valueOrNull?.value

        fun calculate(
            c0: Double,
            c1: Double,
            c2: Double,
            c3: Double,
            c4: Double,
            c5: Double,
            c6: Double,
            c7: Double,
            c8: Double,
            c9: Double
        ) = ((c0 +
                (c1 * mv) +
                (c2 * mv.pow(2.0)) +
                (c3 * mv.pow(3.0)) +
                (c4 * mv.pow(4.0)) +
                (c5 * mv.pow(5.0)) +
                (c6 * mv.pow(6.0)) +
                (c7 * mv.pow(7.0)) +
                (c8 * mv.pow(8.0)) +
                (c9 * mv.pow(9.0))).degreesCelsius +
                (temperatureReferenceValue ?: throw UninitializedPropertyAccessException(noTempRefValueMsg))
                ).toDaqc()

        return Try {
            if (mv >= -5.891 && mv < 0) {
                calculate(0.0, low1, low2, low3, low4, low5, low6, low7, low8, 0.0)
            } else if (mv >= 0 && mv < 20.644) {
                calculate(0.0, mid1, mid2, mid3, mid4, mid5, mid6, mid7, mid8, mid9)
            } else if (mv >= 20.644 && mv < 54.886) {
                calculate(hi0, hi1, hi2, hi3, hi4, hi5, hi6, 0.0, 0.0, 0.0)
            } else {
                throw ValueOutOfRangeException(
                    "Type K thermocouple cannot accurately produce a temperature from" +
                            " voltage ${voltage convertTo Millivolt}"
                )
            }
        }

    }

    public fun cancel() {
        job.cancel()
    }

    public companion object {
        private const val low1 = 25.173462
        private const val low2 = -1.1662878
        private const val low3 = -1.0833638
        private const val low4 = -0.8977354
        private const val low5 = -0.37342377
        private const val low6 = -0.086632643
        private const val low7 = -0.010450598
        private const val low8 = -0.00051920577

        private const val mid1 = 25.08355
        private const val mid2 = 0.07860106
        private const val mid3 = -0.2503131
        private const val mid4 = 0.0831527
        private const val mid5 = -0.01228034
        private const val mid6 = 0.0009804036
        private const val mid7 = -0.0000441303
        private const val mid8 = 0.000001057734
        private const val mid9 = -0.00000001052755

        private const val hi0 = -131.8058
        private const val hi1 = 48.30222
        private const val hi2 = -1.646031
        private const val hi3 = 0.05464731
        private const val hi4 = -0.0009650715
        private const val hi5 = 0.000008802193
        private const val hi6 = -0.00000003110810
    }

}

public fun CoroutineScope.ThermocoupleK(
    channel: AnalogInput<*>,
    acceptableError: Quantity<Temperature> = 1.0.degreesCelsius
) = ThermocoupleK(this, channel, acceptableError)