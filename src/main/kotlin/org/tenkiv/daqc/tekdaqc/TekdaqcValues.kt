package org.tenkiv.daqc.tekdaqc

import com.tenkiv.tekdaqc.hardware.AAnalogInput.Gain
import com.tenkiv.tekdaqc.hardware.AAnalogInput.Rate
import com.tenkiv.tekdaqc.hardware.ATekdaqc.AnalogScale
import org.tenkiv.daqc.LineNoiseFrequency
import org.tenkiv.daqc.lib.ValueOutOfRangeException
import org.tenkiv.physikal.core.*
import tec.uom.se.ComparableQuantity
import tec.uom.se.unit.Units.HERTZ
import javax.measure.quantity.ElectricPotential

val analogNoiseLookupTable =
        mapOf(
                Pair(Gain.X1,
                        mapOf(
                                Pair(Rate.SPS_2_5, 0.3.micro.volt),
                                Pair(Rate.SPS_5, 0.4.micro.volt),
                                Pair(Rate.SPS_10, 0.4.micro.volt),
                                Pair(Rate.SPS_15, 0.5.micro.volt),
                                Pair(Rate.SPS_25, 0.6.micro.volt),
                                Pair(Rate.SPS_30, 0.6.micro.volt),
                                Pair(Rate.SPS_50, 0.7.micro.volt),
                                Pair(Rate.SPS_60, 0.8.micro.volt),
                                Pair(Rate.SPS_100, 1.micro.volt),
                                Pair(Rate.SPS_500, 2.micro.volt),
                                Pair(Rate.SPS_1000, 3.micro.volt),
                                Pair(Rate.SPS_2000, 4.5.micro.volt),
                                Pair(Rate.SPS_3750, 6.micro.volt),
                                Pair(Rate.SPS_7500, 8.micro.volt)
                        )),
                Pair(Gain.X2,
                        mapOf(
                                Pair(Rate.SPS_2_5, 0.2.micro.volt),
                                Pair(Rate.SPS_5, 0.3.micro.volt),
                                Pair(Rate.SPS_10, 0.3.micro.volt),
                                Pair(Rate.SPS_15, 0.3.micro.volt),
                                Pair(Rate.SPS_25, 0.4.micro.volt),
                                Pair(Rate.SPS_30, 0.4.micro.volt),
                                Pair(Rate.SPS_50, 0.5.micro.volt),
                                Pair(Rate.SPS_60, 0.5.micro.volt),
                                Pair(Rate.SPS_100, 0.7.micro.volt),
                                Pair(Rate.SPS_500, 1.5.micro.volt),
                                Pair(Rate.SPS_1000, 2.micro.volt),
                                Pair(Rate.SPS_2000, 3.micro.volt),
                                Pair(Rate.SPS_3750, 4.micro.volt),
                                Pair(Rate.SPS_7500, 5.micro.volt)
                        )),
                Pair(Gain.X4,
                        mapOf(
                                Pair(Rate.SPS_2_5, 0.15.micro.volt),
                                Pair(Rate.SPS_5, 0.15.micro.volt),
                                Pair(Rate.SPS_10, 0.2.micro.volt),
                                Pair(Rate.SPS_15, 0.2.micro.volt),
                                Pair(Rate.SPS_25, 0.25.micro.volt),
                                Pair(Rate.SPS_30, 0.3.micro.volt),
                                Pair(Rate.SPS_50, 0.3.micro.volt),
                                Pair(Rate.SPS_60, 0.4.micro.volt),
                                Pair(Rate.SPS_100, 0.5.micro.volt),
                                Pair(Rate.SPS_500, 0.7.micro.volt),
                                Pair(Rate.SPS_1000, 1.5.micro.volt),
                                Pair(Rate.SPS_2000, 2.micro.volt),
                                Pair(Rate.SPS_3750, 3.micro.volt),
                                Pair(Rate.SPS_7500, 4.micro.volt)
                        )),
                Pair(Gain.X8,
                        mapOf(
                                Pair(Rate.SPS_2_5, 0.1.micro.volt),
                                Pair(Rate.SPS_5, 0.1.micro.volt),
                                Pair(Rate.SPS_10, 0.15.micro.volt),
                                Pair(Rate.SPS_15, 0.15.micro.volt),
                                Pair(Rate.SPS_25, 0.2.micro.volt),
                                Pair(Rate.SPS_30, 0.2.micro.volt),
                                Pair(Rate.SPS_50, 0.25.micro.volt),
                                Pair(Rate.SPS_60, 0.3.micro.volt),
                                Pair(Rate.SPS_100, 0.4.micro.volt),
                                Pair(Rate.SPS_500, 0.7.micro.volt),
                                Pair(Rate.SPS_1000, 1.3.micro.volt),
                                Pair(Rate.SPS_2000, 2.micro.volt),
                                Pair(Rate.SPS_3750, 2.micro.volt),
                                Pair(Rate.SPS_7500, 3.micro.volt)
                        )),
                Pair(Gain.X16,
                        mapOf(
                                Pair(Rate.SPS_2_5, 0.06.micro.volt),
                                Pair(Rate.SPS_5, 0.08.micro.volt),
                                Pair(Rate.SPS_10, 0.1.micro.volt),
                                Pair(Rate.SPS_15, 0.13.micro.volt),
                                Pair(Rate.SPS_25, 0.16.micro.volt),
                                Pair(Rate.SPS_30, 0.16.micro.volt),
                                Pair(Rate.SPS_50, 0.2.micro.volt),
                                Pair(Rate.SPS_60, 0.2.micro.volt),
                                Pair(Rate.SPS_100, 0.25.micro.volt),
                                Pair(Rate.SPS_500, 0.6.micro.volt),
                                Pair(Rate.SPS_1000, 0.8.micro.volt),
                                Pair(Rate.SPS_2000, 1.micro.volt),
                                Pair(Rate.SPS_3750, 1.5.micro.volt),
                                Pair(Rate.SPS_7500, 2.micro.volt)
                        )),
                Pair(Gain.X32,
                        mapOf(
                                Pair(Rate.SPS_2_5, 0.05.micro.volt),
                                Pair(Rate.SPS_5, 0.06.micro.volt),
                                Pair(Rate.SPS_10, 0.07.micro.volt),
                                Pair(Rate.SPS_15, 0.1.micro.volt),
                                Pair(Rate.SPS_25, 0.14.micro.volt),
                                Pair(Rate.SPS_30, 0.14.micro.volt),
                                Pair(Rate.SPS_50, 0.15.micro.volt),
                                Pair(Rate.SPS_60, 0.16.micro.volt),
                                Pair(Rate.SPS_100, 0.2.micro.volt),
                                Pair(Rate.SPS_500, 0.5.micro.volt),
                                Pair(Rate.SPS_1000, 0.6.micro.volt),
                                Pair(Rate.SPS_2000, 0.8.micro.volt),
                                Pair(Rate.SPS_3750, 1.micro.volt),
                                Pair(Rate.SPS_7500, 1.5.micro.volt)
                        )),
                Pair(Gain.X64,
                        mapOf(
                                Pair(Rate.SPS_2_5, 0.05.micro.volt),
                                Pair(Rate.SPS_5, 0.06.micro.volt),
                                Pair(Rate.SPS_10, 0.07.micro.volt),
                                Pair(Rate.SPS_15, 0.09.micro.volt),
                                Pair(Rate.SPS_25, 0.1.micro.volt),
                                Pair(Rate.SPS_30, 0.12.micro.volt),
                                Pair(Rate.SPS_50, 0.14.micro.volt),
                                Pair(Rate.SPS_60, 0.15.micro.volt),
                                Pair(Rate.SPS_100, 0.19.micro.volt),
                                Pair(Rate.SPS_500, 0.4.micro.volt),
                                Pair(Rate.SPS_1000, 0.5.micro.volt),
                                Pair(Rate.SPS_2000, 0.7.micro.volt),
                                Pair(Rate.SPS_3750, 1.micro.volt),
                                Pair(Rate.SPS_7500, 1.3.micro.volt)
                        ))
        )

fun getFastestRateForAccuracy(
        gain: Gain,
        analogScale: AnalogScale,
        maximumError: ComparableQuantity<ElectricPotential>,
        lineFrequency: LineNoiseFrequency): Rate {

    val scaler = if (analogScale == AnalogScale.ANALOG_SCALE_400V) 80 else 1

    val acceptableRates = analogNoiseLookupTable[gain]?.filter { it.value < (maximumError * scaler) }

    if (acceptableRates == null || acceptableRates.isEmpty()) {
        throw ValueOutOfRangeException("No Possible Rates for Demanded Accuracy and Max Voltage")
    } else {
        val list = if (lineFrequency is LineNoiseFrequency.AccountFor) {
            acceptableRates.keys.filter {
                (lineFrequency.frequency tu HERTZ).toFloat().approxDivisibleBy(it.rate.toFloat())
            }
        } else {
            acceptableRates.keys
        }

        return list.sortedByDescending { it.rate.toFloat() }.first()
    }
}

fun Float.approxDivisibleBy(number: Float): Boolean {
    val mod = (this % number)
    if (mod > 0) {
        mod * -1
    }
    return (mod <= .1f)
}