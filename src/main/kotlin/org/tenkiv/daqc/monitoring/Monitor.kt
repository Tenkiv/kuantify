package org.tenkiv.daqc.monitoring

import org.tenkiv.daqc.ArffDataSetStub
import org.tenkiv.daqc.DaqcValue

abstract class Monitor<I : DaqcValue, O : DaqcValue, D>(var desiredValue: D,
                                                        val previousDataSet: ArffDataSetStub = ArffDataSetStub(0)) {


    abstract fun updateDataSet()

}