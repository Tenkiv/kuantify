package org.tenkiv.daqc.hardware.outputs

import org.tenkiv.daqc.DaqcQuantity
import org.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import javax.measure.quantity.Dimensionless


class SimplePwmController internal constructor(digitalOutput: DigitalOutput)
    : ScPwmController<Dimensionless>(digitalOutput) {

    override fun convertOutput(setting: DaqcQuantity<Dimensionless>) = setting

}