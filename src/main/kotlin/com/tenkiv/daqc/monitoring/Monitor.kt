package com.tenkiv.daqc.monitoring

import com.tenkiv.daqc.ArffDataSetStub
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.Controller
import com.tenkiv.daqc.hardware.Sensor

/**
 * Created by tenkiv on 3/20/17.
 */
abstract class Monitor<I : DaqcValue, O : DaqcValue, D>(val requiredSensors: Array<Sensor<I>>,
                                                        val requiredControllers: Array<Controller<O>>,
                                                        var desiredValue: D,
                                                        val previousDataSet: ArffDataSetStub = ArffDataSetStub(0)) {


    abstract fun updateDataSet()

}