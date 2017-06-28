package com.tenkiv

import com.tenkiv.daqc.networking.Locator
import com.tenkiv.daqc.networking.RemoteLocator
import kotlinx.coroutines.experimental.newSingleThreadContext
import okhttp3.internal.framed.Settings
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by tenkiv on 6/10/17.
 */
object Daqc{
    fun initiate(coroutineContext: CoroutineContext, someSettings: Settings): Locator{

    }
}

private var _context: CoroutineContext? = null

private fun getNewContext(): CoroutineContext {
    val context = newSingleThreadContext("Main Daqc Context")
    _context = context
    return context
}

var DAQC_CONTEXT: CoroutineContext
    get() {
        return _context ?: getNewContext()
    }
    set(value) {
        _context = value
    }