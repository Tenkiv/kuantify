package com.tenkiv.daqc.hardware.definitions

import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.UpdatableListener
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Created by tenkiv on 4/11/17.
 */
interface Updatable<T: DaqcValue> {

    val listeners: MutableList<UpdatableListener<T>>

    var value: T?

}