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

package org.tenkiv.kuantify.networking

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import org.tenkiv.kuantify.Updatable

/**
 * Class to wrap multiple locators into a single broadcast channel.
 * Starts automatically upon creation.
 *
 * @param locators The [DeviceLocator]s to be included.
 */
class CombinationLocator(vararg val locators: DeviceLocator<*>) : Updatable<LocatorUpdate<*>>, CoroutineScope {

    @Volatile
    private var job = Job()

    private val _broadcastChannel = ConflatedBroadcastChannel<LocatorUpdate<*>>()

    override val coroutineContext get() = Dispatchers.Default + job

    override val broadcastChannel: ConflatedBroadcastChannel<out LocatorUpdate<*>>
        get() = _broadcastChannel

    init {
        start()
    }

    /**
     * Starts the combination locator, returning false if it was already running.
     */
    fun start(): Boolean = if (isActive) {
        false
    } else {
        job = Job()
        locators.forEach { locator ->
            launch {
                locator.broadcastChannel.consumeEach { device -> _broadcastChannel.send(device) }
            }
        }
        true
    }

    /**
     * Stops the combination locator, returning false if it was already stopped.
     */
    fun stop(): Boolean = if (!isActive) {
        false
    } else {
        job.cancel()
        true
    }
}