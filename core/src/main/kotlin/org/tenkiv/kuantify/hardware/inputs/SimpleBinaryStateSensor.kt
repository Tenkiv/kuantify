/*
 * Copyright 2018 Tenkiv, Inc.
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

package org.tenkiv.kuantify.hardware.inputs

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import org.tenkiv.coral.at
import org.tenkiv.kuantify.BinaryStateMeasurement
import org.tenkiv.kuantify.data.BinaryState
import org.tenkiv.kuantify.gate.acquire.input.BinaryStateInput
import org.tenkiv.kuantify.hardware.definitions.channel.DigitalInput
import org.tenkiv.kuantify.lib.openNewCoroutineListener

/**
 * A simple simple implementation of a binary sensor
 *
 * @param digitalInput The [DigitalInput] that is being read from.
 */
class SimpleBinaryStateSensor internal constructor(val digitalInput: DigitalInput) :
    BinaryStateInput {

    /**
     * Denotes if the [DigitalInput] is inverted; ie Off = [BinaryState.On]
     */
    @Volatile
    var inverted: Boolean = false

    private val _broadcastChannel = ConflatedBroadcastChannel<BinaryStateMeasurement>()

    override val broadcastChannel: ConflatedBroadcastChannel<out BinaryStateMeasurement>
        get() = _broadcastChannel

    override val failureBroadcastChannel get() = digitalInput.failureBroadcastChannel

    override val isActive get() = digitalInput.isActiveForBinaryState

    override val updateRate get() = digitalInput.updateRate

    init {
        digitalInput.broadcastChannel.openNewCoroutineListener(CommonPool) { measurement ->
            if (!inverted) _broadcastChannel.send(measurement) else when (measurement.value) {
                BinaryState.On -> _broadcastChannel.send(BinaryState.Off at measurement.instant)
                BinaryState.Off -> _broadcastChannel.send(BinaryState.On at measurement.instant)
            }
        }
    }

    override fun activate() = digitalInput.activateForCurrentState()

    override fun deactivate() = digitalInput.deactivate()
}