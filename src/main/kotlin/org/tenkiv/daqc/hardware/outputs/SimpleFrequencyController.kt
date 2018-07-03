package org.tenkiv.daqc.hardware.outputs

import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import javax.measure.quantity.Frequency

class SimpleFrequencyController internal constructor(digitalOutput: DigitalOutput) :
    ScDigitalFrequencyController<Frequency>(digitalOutput) {

    override fun convertOutput(setting: DaqcQuantity<Frequency>) = setting

}