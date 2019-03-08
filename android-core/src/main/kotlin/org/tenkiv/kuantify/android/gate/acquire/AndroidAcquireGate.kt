/*
 * Copyright 2019 Tenkiv, Inc.
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
 *
 */

package org.tenkiv.kuantify.android.gate.acquire

import kotlinx.coroutines.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.android.gate.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.gate.acquire.*
import org.tenkiv.kuantify.gate.acquire.*
import org.tenkiv.kuantify.gate.acquire.input.*
import javax.measure.*
import kotlin.reflect.*

typealias AndroidQuantityInput<Q> = AndroidInput<DaqcQuantity<Q>>

interface AndroidAcquireGate<T : DaqcData> : AndroidDaqcGate<T>, AcquireGate<T>

interface AndroidInput<T : DaqcValue> : AndroidAcquireGate<T>, Input<T>

class AndroidRemoteQuantityInput<Q : Quantity<Q>> internal constructor(
    scope: CoroutineScope,
    uid: String,
    override val quantityType: KClass<Q>
) : FSRemoteQuantityInput<Q>(scope.coroutineContext, uid), AndroidQuantityInput<Q> {

    override val updateRate: UpdateRate by runningAverage()
}

class AndroidRemoteBinaryStateInput internal constructor(
    scope: CoroutineScope,
    uid: String
) : FSRemoteBinaryStateInput(scope.coroutineContext, uid), AndroidInput<BinaryState> {

    override val updateRate: UpdateRate by runningAverage()

}
