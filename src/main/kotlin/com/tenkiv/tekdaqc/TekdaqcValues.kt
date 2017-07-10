package com.tenkiv.tekdaqc

import com.tenkiv.daqc.LineNoiseFrequency
import com.tenkiv.daqc.ValueOutOfRangeException
import com.tenkiv.tekdaqc.hardware.AAnalogInput.Gain
import com.tenkiv.tekdaqc.hardware.AAnalogInput.Rate
import com.tenkiv.tekdaqc.hardware.ATekdaqc.AnalogScale
import org.tenkiv.physikal.core.*
import tec.uom.se.ComparableQuantity
import tec.uom.se.unit.Units.HERTZ
import javax.measure.quantity.ElectricPotential

val analogNoiseLookupTable =
        mapOf(
                Pair(Gain.X1,
                        mapOf(
                            Pair(Rate.SPS_2_5,.3.micro.volt),
                                Pair(Rate.SPS_5,.4.micro.volt),
                                Pair(Rate.SPS_10,.4.micro.volt),
                                Pair(Rate.SPS_15,.5.micro.volt),
                                Pair(Rate.SPS_25,.6.micro.volt),
                                Pair(Rate.SPS_30,.6.micro.volt),
                                Pair(Rate.SPS_50,.7.micro.volt),
                                Pair(Rate.SPS_60,.8.micro.volt),
                                Pair(Rate.SPS_100,1.micro.volt),
                                Pair(Rate.SPS_500,2.micro.volt),
                                Pair(Rate.SPS_1000,3.micro.volt),
                                Pair(Rate.SPS_2000,4.5.micro.volt),
                                Pair(Rate.SPS_3750,6.micro.volt),
                                Pair(Rate.SPS_7500,8.micro.volt)
                                )),
                Pair(Gain.X2,
                        mapOf(
                                Pair(Rate.SPS_2_5,.2.micro.volt),
                                Pair(Rate.SPS_5,.3.micro.volt),
                                Pair(Rate.SPS_10,.3.micro.volt),
                                Pair(Rate.SPS_15,.3.micro.volt),
                                Pair(Rate.SPS_25,.4.micro.volt),
                                Pair(Rate.SPS_30,.4.micro.volt),
                                Pair(Rate.SPS_50,.5.micro.volt),
                                Pair(Rate.SPS_60,.5.micro.volt),
                                Pair(Rate.SPS_100,.7.micro.volt),
                                Pair(Rate.SPS_500,1.5.micro.volt),
                                Pair(Rate.SPS_1000,2.micro.volt),
                                Pair(Rate.SPS_2000,3.micro.volt),
                                Pair(Rate.SPS_3750,4.micro.volt),
                                Pair(Rate.SPS_7500,5.micro.volt)
                        )),
                Pair(Gain.X4,
                        mapOf(
                                Pair(Rate.SPS_2_5,.15.micro.volt),
                                Pair(Rate.SPS_5,.15.micro.volt),
                                Pair(Rate.SPS_10,.2.micro.volt),
                                Pair(Rate.SPS_15,.2.micro.volt),
                                Pair(Rate.SPS_25,.25.micro.volt),
                                Pair(Rate.SPS_30,.3.micro.volt),
                                Pair(Rate.SPS_50,.3.micro.volt),
                                Pair(Rate.SPS_60,.4.micro.volt),
                                Pair(Rate.SPS_100,.5.micro.volt),
                                Pair(Rate.SPS_500,.7.micro.volt),
                                Pair(Rate.SPS_1000,1.5.micro.volt),
                                Pair(Rate.SPS_2000,2.micro.volt),
                                Pair(Rate.SPS_3750,3.micro.volt),
                                Pair(Rate.SPS_7500,4.micro.volt)
                        )),
                Pair(Gain.X8,
                        mapOf(
                                Pair(Rate.SPS_2_5,.1.micro.volt),
                                Pair(Rate.SPS_5,.1.micro.volt),
                                Pair(Rate.SPS_10,.15.micro.volt),
                                Pair(Rate.SPS_15,.15.micro.volt),
                                Pair(Rate.SPS_25,.2.micro.volt),
                                Pair(Rate.SPS_30,.2.micro.volt),
                                Pair(Rate.SPS_50,.25.micro.volt),
                                Pair(Rate.SPS_60,.3.micro.volt),
                                Pair(Rate.SPS_100,.4.micro.volt),
                                Pair(Rate.SPS_500,.7.micro.volt),
                                Pair(Rate.SPS_1000,1.3.micro.volt),
                                Pair(Rate.SPS_2000,2.micro.volt),
                                Pair(Rate.SPS_3750,2.micro.volt),
                                Pair(Rate.SPS_7500,3.micro.volt)
                        )),
                Pair(Gain.X16,
                        mapOf(
                                Pair(Rate.SPS_2_5,.06.micro.volt),
                                Pair(Rate.SPS_5,.08.micro.volt),
                                Pair(Rate.SPS_10,.1.micro.volt),
                                Pair(Rate.SPS_15,.13.micro.volt),
                                Pair(Rate.SPS_25,.16.micro.volt),
                                Pair(Rate.SPS_30,.16.micro.volt),
                                Pair(Rate.SPS_50,.2.micro.volt),
                                Pair(Rate.SPS_60,.2.micro.volt),
                                Pair(Rate.SPS_100,.25.micro.volt),
                                Pair(Rate.SPS_500,.6.micro.volt),
                                Pair(Rate.SPS_1000,.8.micro.volt),
                                Pair(Rate.SPS_2000,1.micro.volt),
                                Pair(Rate.SPS_3750,1.5.micro.volt),
                                Pair(Rate.SPS_7500,2.micro.volt)
                        )),
                Pair(Gain.X32,
                        mapOf(
                                Pair(Rate.SPS_2_5,.05.micro.volt),
                                Pair(Rate.SPS_5,.06.micro.volt),
                                Pair(Rate.SPS_10,.07.micro.volt),
                                Pair(Rate.SPS_15,.1.micro.volt),
                                Pair(Rate.SPS_25,.14.micro.volt),
                                Pair(Rate.SPS_30,.14.micro.volt),
                                Pair(Rate.SPS_50,.15.micro.volt),
                                Pair(Rate.SPS_60,.16.micro.volt),
                                Pair(Rate.SPS_100,.2.micro.volt),
                                Pair(Rate.SPS_500,.5.micro.volt),
                                Pair(Rate.SPS_1000,.6.micro.volt),
                                Pair(Rate.SPS_2000,.8.micro.volt),
                                Pair(Rate.SPS_3750,1.micro.volt),
                                Pair(Rate.SPS_7500,1.5.micro.volt)
                        )),
                Pair(Gain.X64,
                        mapOf(
                                Pair(Rate.SPS_2_5,.05.micro.volt),
                                Pair(Rate.SPS_5,.06.micro.volt),
                                Pair(Rate.SPS_10,.07.micro.volt),
                                Pair(Rate.SPS_15,.09.micro.volt),
                                Pair(Rate.SPS_25,.1.micro.volt),
                                Pair(Rate.SPS_30,.12.micro.volt),
                                Pair(Rate.SPS_50,.14.micro.volt),
                                Pair(Rate.SPS_60,.15.micro.volt),
                                Pair(Rate.SPS_100,.19.micro.volt),
                                Pair(Rate.SPS_500,.4.micro.volt),
                                Pair(Rate.SPS_1000,.5.micro.volt),
                                Pair(Rate.SPS_2000,.7.micro.volt),
                                Pair(Rate.SPS_3750,1.micro.volt),
                                Pair(Rate.SPS_7500,1.3.micro.volt)
                        ))
        )

fun getFastestRateForAccuracy(
        gain: Gain,
        analogScale: AnalogScale,
        maximumError: ComparableQuantity<ElectricPotential>,
        lineFrequency: LineNoiseFrequency): Rate{

    val scaler = if(analogScale == AnalogScale.ANALOG_SCALE_400V){ 80 } else { 1 }

    val acceptableRates = analogNoiseLookupTable[gain]?.filter { it.value < (maximumError * scaler) }

    if(acceptableRates == null || acceptableRates.isEmpty()){
        throw ValueOutOfRangeException("No Possible Rates for Demanded Accuracy and Max Voltage")
    }else{
        val list = if(lineFrequency is LineNoiseFrequency.AccountFor) {
            acceptableRates.keys.filter {
                it.rate.toFloat().approxDivisibleBy((lineFrequency.frequency tu HERTZ).toFloat())
            }
        }else{
            acceptableRates.keys
        }

        return list.sortedByDescending { it.rate.toFloat() }.first()
    }
}

fun Float.approxDivisibleBy(number: Float): Boolean{
    val mod = (this%number)
    if(mod > 0){ mod * -1}
    return (mod <= .01f)
}