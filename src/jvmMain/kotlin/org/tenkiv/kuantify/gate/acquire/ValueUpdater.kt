/*
 * Copyright 2020 Tenkiv, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.tenkiv.kuantify.gate.acquire

//import kotlinx.coroutines.*
//import kotlinx.coroutines.flow.*
//import org.tenkiv.coral.*
//import org.tenkiv.kuantify.data.*
//import kotlin.properties.*
//import kotlin.reflect.*
//
//public fun <T : DaqcData> AcquireGate<T>.valueUpdater(): ReceiveGateValueDelegate<T> =
//    ReceiveGateValueDelegate(this)
//
//public class ReceiveGateValueDelegate<T: DaqcData> internal constructor(gate: AcquireGate<T>) :
//    ReadOnlyProperty<AcquireGate<T>, ValueInstant<T>?>, CoroutineScope by gate {
//    @Volatile
//    private var value: ValueInstant<T>? = null
//
//    private val updaterJob = launch(start = CoroutineStart.LAZY) {
//        gate.updateBroadcaster.collect {
//            value = it
//        }
//    }
//
//    public override fun getValue(thisRef: AcquireGate<T>, property: KProperty<*>): ValueInstant<T>? =
//        if (value != null) {
//            value
//        } else {
//            updaterJob.start()
//            value
//        }
//}
