package com.tenkiv.daqc.hardware

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.definitions.channel.*

/**
 * Created by tenkiv on 4/5/17.
 */
abstract class Controller<T: DaqcValue>(val requiredAnalogOutputs: Array<AnalogOutput>,
                                        val requiredDigitalOutputs: Array<DigitalOutput>): Output<T>{

    // Set State get Implemented for toggling on/off or variations?
}