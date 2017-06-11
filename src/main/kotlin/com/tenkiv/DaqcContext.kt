package com.tenkiv

import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlin.coroutines.experimental.CoroutineContext

/**
 * Created by tenkiv on 6/10/17.
 */

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