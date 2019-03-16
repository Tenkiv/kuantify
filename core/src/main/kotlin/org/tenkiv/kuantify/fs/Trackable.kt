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

package org.tenkiv.kuantify.fs

import kotlinx.serialization.json.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.fs.gate.acquire.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.lib.*
import org.tenkiv.kuantify.networking.configuration.*
import org.tenkiv.physikal.core.*
import tec.units.indriya.*
import javax.measure.quantity.*
import kotlin.properties.*
import kotlin.reflect.*

fun FSRemoteAcquireGate<*, *>.configuredUpdateRateDelegate(): FSRemoteConfiguredUpdateRateDelegate =
    FSRemoteConfiguredUpdateRateDelegate(this)

class FSRemoteConfiguredUpdateRateDelegate internal constructor(acquireGate: FSRemoteAcquireGate<*, *>) :
    ReadOnlyProperty<FSRemoteAcquireGate<*, *>, UpdateRate.Configured> {

    private val updatable = acquireGate.Updatable<ComparableQuantity<Frequency>>()

    private val updateRate = UpdateRate.Configured(updatable)

    fun addToRoute(routing: SideNetworkRouting<String>) {
        routing.bind<ComparableQuantity<Frequency>>(RC.UPDATE_RATE) {
            receive {
                val value = Json.parse(
                    ComparableQuantitySerializer,
                    it
                ).asType<Frequency>()
                updatable.set(value)
            }
        }
    }

    override fun getValue(thisRef: FSRemoteAcquireGate<*, *>, property: KProperty<*>): UpdateRate.Configured =
        updateRate

}