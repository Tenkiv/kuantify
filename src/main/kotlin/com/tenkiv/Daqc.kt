package com.tenkiv

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.LineNoiseFrequency
import com.tenkiv.daqc.networking.Locator
import kotlinx.coroutines.experimental.newSingleThreadContext
import okhttp3.internal.framed.Settings
import org.tenkiv.coral.ValueInstant
import org.tenkiv.physikal.core.hertz
import tec.uom.se.ComparableQuantity
import javax.measure.quantity.ElectricPotential
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by tenkiv on 6/10/17.
 */
typealias Measurement = ValueInstant<DaqcValue>
typealias BinaryMeasurement = ValueInstant<BinaryState>

object Daqc{
    fun initiate(coroutineContext: CoroutineContext = getNewContext(),
                 lineNoiseFrequency: LineNoiseFrequency =
                 LineNoiseFrequency.AccountFor(50.hertz)): Locator{
        DAQC_CONTEXT = coroutineContext
        return Locator()
    }
}

private var _context: CoroutineContext? = null

private fun getNewContext(): CoroutineContext {
    val context = newSingleThreadContext("Main Daqc Context")
    _context = context
    return context
}

var DAQC_CONTEXT: CoroutineContext
    get() { return _context ?: getNewContext() }
    set(value) { _context = value }