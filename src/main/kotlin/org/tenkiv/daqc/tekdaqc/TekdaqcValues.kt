package org.tenkiv.daqc.tekdaqc

import com.tenkiv.tekdaqc.hardware.AAnalogInput.Gain
import com.tenkiv.tekdaqc.hardware.AAnalogInput.Rate
import com.tenkiv.tekdaqc.hardware.ATekdaqc.AnalogScale
import org.tenkiv.daqc.LineNoiseFrequency
import org.tenkiv.daqc.lib.ValueOutOfRangeException
import org.tenkiv.physikal.core.micro
import org.tenkiv.physikal.core.times
import org.tenkiv.physikal.core.toFloatIn
import org.tenkiv.physikal.core.volt
import tec.uom.se.ComparableQuantity
import tec.uom.se.unit.Units.HERTZ
import javax.measure.quantity.ElectricPotential

private val analogNoiseLookupTable =
    mapOf(
        Gain.X1 to mapOf(
            Rate.SPS_2_5 to 0.3.micro.volt,
            Rate.SPS_5 to 0.4.micro.volt,
            Rate.SPS_10 to 0.4.micro.volt,
            Rate.SPS_15 to 0.5.micro.volt,
            Rate.SPS_25 to 0.6.micro.volt,
            Rate.SPS_30 to 0.6.micro.volt,
            Rate.SPS_50 to 0.7.micro.volt,
            Rate.SPS_60 to 0.8.micro.volt,
            Rate.SPS_100 to 1.micro.volt,
            Rate.SPS_500 to 2.micro.volt,
            Rate.SPS_1000 to 3.micro.volt,
            Rate.SPS_2000 to 4.5.micro.volt,
            Rate.SPS_3750 to 6.micro.volt,
            Rate.SPS_7500 to 8.micro.volt
        ),
        Gain.X2 to mapOf(
            Rate.SPS_2_5 to 0.2.micro.volt,
            Rate.SPS_5 to 0.3.micro.volt,
            Rate.SPS_10 to 0.3.micro.volt,
            Rate.SPS_15 to 0.3.micro.volt,
            Rate.SPS_25 to 0.4.micro.volt,
            Rate.SPS_30 to 0.4.micro.volt,
            Rate.SPS_50 to 0.5.micro.volt,
            Rate.SPS_60 to 0.5.micro.volt,
            Rate.SPS_100 to 0.7.micro.volt,
            Rate.SPS_500 to 1.5.micro.volt,
            Rate.SPS_1000 to 2.micro.volt,
            Rate.SPS_2000 to 3.micro.volt,
            Rate.SPS_3750 to 4.micro.volt,
            Rate.SPS_7500 to 5.micro.volt
        ),
        Gain.X4 to mapOf(
            Rate.SPS_2_5 to 0.15.micro.volt,
            Rate.SPS_5 to 0.15.micro.volt,
            Rate.SPS_10 to 0.2.micro.volt,
            Rate.SPS_15 to 0.2.micro.volt,
            Rate.SPS_25 to 0.25.micro.volt,
            Rate.SPS_30 to 0.3.micro.volt,
            Rate.SPS_50 to 0.3.micro.volt,
            Rate.SPS_60 to 0.4.micro.volt,
            Rate.SPS_100 to 0.5.micro.volt,
            Rate.SPS_500 to 0.7.micro.volt,
            Rate.SPS_1000 to 1.5.micro.volt,
            Rate.SPS_2000 to 2.micro.volt,
            Rate.SPS_3750 to 3.micro.volt,
            Rate.SPS_7500 to 4.micro.volt
        ),
        Gain.X8 to mapOf(
            Rate.SPS_2_5 to 0.1.micro.volt,
            Rate.SPS_5 to 0.1.micro.volt,
            Rate.SPS_10 to 0.15.micro.volt,
            Rate.SPS_15 to 0.15.micro.volt,
            Rate.SPS_25 to 0.2.micro.volt,
            Rate.SPS_30 to 0.2.micro.volt,
            Rate.SPS_50 to 0.25.micro.volt,
            Rate.SPS_60 to 0.3.micro.volt,
            Rate.SPS_100 to 0.4.micro.volt,
            Rate.SPS_500 to 0.7.micro.volt,
            Rate.SPS_1000 to 1.3.micro.volt,
            Rate.SPS_2000 to 2.micro.volt,
            Rate.SPS_3750 to 2.micro.volt,
            Rate.SPS_7500 to 3.micro.volt
        ),
        Gain.X16 to mapOf(
            Rate.SPS_2_5 to 0.06.micro.volt,
            Rate.SPS_5 to 0.08.micro.volt,
            Rate.SPS_10 to 0.1.micro.volt,
            Rate.SPS_15 to 0.13.micro.volt,
            Rate.SPS_25 to 0.16.micro.volt,
            Rate.SPS_30 to 0.16.micro.volt,
            Rate.SPS_50 to 0.2.micro.volt,
            Rate.SPS_60 to 0.2.micro.volt,
            Rate.SPS_100 to 0.25.micro.volt,
            Rate.SPS_500 to 0.6.micro.volt,
            Rate.SPS_1000 to 0.8.micro.volt,
            Rate.SPS_2000 to 1.micro.volt,
            Rate.SPS_3750 to 1.5.micro.volt,
            Rate.SPS_7500 to 2.micro.volt
        ),
        Gain.X32 to mapOf(
            Rate.SPS_2_5 to 0.05.micro.volt,
            Rate.SPS_5 to 0.06.micro.volt,
            Rate.SPS_10 to 0.07.micro.volt,
            Rate.SPS_15 to 0.1.micro.volt,
            Rate.SPS_25 to 0.14.micro.volt,
            Rate.SPS_30 to 0.14.micro.volt,
            Rate.SPS_50 to 0.15.micro.volt,
            Rate.SPS_60 to 0.16.micro.volt,
            Rate.SPS_100 to 0.2.micro.volt,
            Rate.SPS_500 to 0.5.micro.volt,
            Rate.SPS_1000 to 0.6.micro.volt,
            Rate.SPS_2000 to 0.8.micro.volt,
            Rate.SPS_3750 to 1.micro.volt,
            Rate.SPS_7500 to 1.5.micro.volt
        ),
        Gain.X64 to mapOf(
            Rate.SPS_2_5 to 0.05.micro.volt,
            Rate.SPS_5 to 0.06.micro.volt,
            Rate.SPS_10 to 0.07.micro.volt,
            Rate.SPS_15 to 0.09.micro.volt,
            Rate.SPS_25 to 0.1.micro.volt,
            Rate.SPS_30 to 0.12.micro.volt,
            Rate.SPS_50 to 0.14.micro.volt,
            Rate.SPS_60 to 0.15.micro.volt,
            Rate.SPS_100 to 0.19.micro.volt,
            Rate.SPS_500 to 0.4.micro.volt,
            Rate.SPS_1000 to 0.5.micro.volt,
            Rate.SPS_2000 to 0.7.micro.volt,
            Rate.SPS_3750 to 1.micro.volt,
            Rate.SPS_7500 to 1.3.micro.volt
        )
    )

fun getFastestRateForAccuracy(
    gain: Gain,
    analogScale: AnalogScale,
    maximumError: ComparableQuantity<ElectricPotential>,
    lineFrequency: LineNoiseFrequency
): Rate {

    val scaler = if (analogScale == AnalogScale.ANALOG_SCALE_400V) 80 else 1

    val acceptableRates = analogNoiseLookupTable[gain]?.filter { it.value < (maximumError * scaler) }

    if (acceptableRates == null || acceptableRates.isEmpty()) {
        throw ValueOutOfRangeException("No Possible Rates for Demanded Accuracy and Max Voltage")
    } else {
        val list = if (lineFrequency is LineNoiseFrequency.AccountFor) {
            acceptableRates.keys.filter {
                (lineFrequency.frequency.toFloatIn(HERTZ).approxDivisibleBy(it.rate.toFloat()))
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