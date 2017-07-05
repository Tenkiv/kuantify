package com.tenkiv.daqc.hardware

import com.tenkiv.daqc.DaqcQuantity
import com.tenkiv.daqc.hardware.definitions.channel.DigitalOutput
import javax.measure.quantity.Dimensionless


class SimplePwmController(digitalOutput: DigitalOutput) : ScPwmController<Dimensionless>(digitalOutput) {

    override fun convertOutput(setting: DaqcQuantity<Dimensionless>): DaqcQuantity<Dimensionless> = setting
    
}