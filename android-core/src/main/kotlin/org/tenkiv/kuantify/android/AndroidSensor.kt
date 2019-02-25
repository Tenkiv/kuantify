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

package org.tenkiv.kuantify.android

import kotlinx.coroutines.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.acquire.input.*
import javax.measure.*
import kotlin.reflect.*

typealias QuantityAndroidSensor<Q> = AndroidSensor<DaqcQuantity<Q>>

interface AndroidSensor<T : DaqcValue> : Input<T> {
    val uid: String
}

class RemoteQuantityAndroidSensor<Q : Quantity<Q>> internal constructor(
    scope: CoroutineScope,
    uid: String,
    override val quantityType: KClass<Q>
) : FSRemoteQuantityInput<Q>(scope.coroutineContext, uid), QuantityAndroidSensor<Q> {

    override val updateRate: UpdateRate by runningAverage()
}

class RemoteBinaryStateAndroidSensor internal constructor(
    scope: CoroutineScope,
    uid: String
) : FSRemoteBinaryStateInput(scope.coroutineContext, uid), AndroidSensor<BinaryState> {

    override val updateRate: UpdateRate by runningAverage()

}

object AndroidSensorTypeId {
    const val AMBIENT_TEMPERATURE = "AT"
    const val HEART_RATE = "HR"
    const val LIGHT = "LI"
    const val PROXIMITY = "PX"
    const val PRESSURE = "PS"
    const val RELATIVE_HUMIDITY = "HU"
}