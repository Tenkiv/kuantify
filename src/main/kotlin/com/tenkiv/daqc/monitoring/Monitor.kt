package com.tenkiv.daqc.monitoring

import com.tenkiv.daqc.ArffDataSetStub
import com.tenkiv.daqc.DaqcValue

abstract class Monitor<I : DaqcValue, O : DaqcValue, D>(var desiredValue: D,
                                                        val previousDataSet: ArffDataSetStub = ArffDataSetStub(0)) {


    abstract fun updateDataSet()

}