package com.tenkiv

import com.tenkiv.daqc.BinaryState
import com.tenkiv.daqc.DaqcValue
import com.tenkiv.daqc.networking.Locator
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.tenkiv.coral.ValueInstant
import kotlin.coroutines.experimental.CoroutineContext

typealias Measurement = ValueInstant<DaqcValue>
typealias BinaryMeasurement = ValueInstant<BinaryState>

class Daqc(){
    fun initiate(coroutineContext: CoroutineContext = getNewContext(),
                 createBoardSpecificContext: Boolean = false): Locator{

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

data class LocatorParameters internal constructor(val boardSpecificContext:Boolean)