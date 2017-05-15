package com.tenkiv.daqc.hardware.definitions.channel

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import com.tenkiv.daqc.hardware.definitions.Channel
import com.tenkiv.daqc.hardware.definitions.HardwareType
import com.tenkiv.daqc.hardware.definitions.channel.Output
import java.util.concurrent.CopyOnWriteArrayList
import javax.measure.Quantity
import javax.measure.quantity.ElectricPotential

/**
 * Created by tenkiv on 3/20/17.
 */
abstract class AnalogOutput: Output<DaqcValue.Quantity<ElectricPotential>>, Channel<DaqcValue.Quantity<ElectricPotential>> {

    override val listeners: MutableList<UpdatableListener<DaqcValue.Quantity<ElectricPotential>>> = CopyOnWriteArrayList()

    private var _value: DaqcValue.Quantity<ElectricPotential>? = null

    override var value: DaqcValue.Quantity<ElectricPotential>?
        get() = _value
        set(value) { _value = value; listeners.forEach{ it.onUpdate(this) } }

}