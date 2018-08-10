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

package org.tenkiv.daqc.networking

import kotlinx.coroutines.experimental.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.experimental.channels.consumeEach
import kotlinx.coroutines.experimental.launch
import org.tenkiv.daqc.Updatable
import org.tenkiv.daqc.daqcThreadContext

// Locators can be turned into a private val if we decide to add get specific device functions to this class later.
class CombinationLocator(vararg locators: DeviceLocator<*>) : Updatable<LocatorUpdate<*>> {

    private val _broadcastChannel = ConflatedBroadcastChannel<LocatorUpdate<*>>()

    override val broadcastChannel: ConflatedBroadcastChannel<out LocatorUpdate<*>>
        get() = _broadcastChannel

    init {
        locators.forEach { locator ->
            launch(daqcThreadContext) {
                locator.broadcastChannel.consumeEach { device -> _broadcastChannel.offer(device) }
            }
        }
    }

}