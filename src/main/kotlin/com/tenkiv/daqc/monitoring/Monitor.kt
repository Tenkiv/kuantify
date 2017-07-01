package com.tenkiv.daqc.monitoring

import com.tenkiv.daqc.ArffDataSetStub
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.hardware.SingleChannelAnalogSensor

/**
 * Created by tenkiv on 3/20/17.
 */
abstract class Monitor<I : DaqcValue, O : DaqcValue, D>(var desiredValue: D,
                                                        val previousDataSet: ArffDataSetStub = ArffDataSetStub(0)) {


    abstract fun updateDataSet()

}