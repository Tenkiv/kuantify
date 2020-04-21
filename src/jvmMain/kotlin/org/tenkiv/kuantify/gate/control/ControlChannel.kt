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

package org.tenkiv.kuantify.gate.control

import kotlinx.coroutines.*
import kotlinx.coroutines.time.*
import kotlinx.coroutines.withTimeout
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.gate.*
import org.tenkiv.kuantify.lib.*
import kotlin.time.*

public interface ControlChannel<T : DaqcData> : DaqcChannel<T> {

    /**
     * Sets the output if the function does not encounter a [SettingProblem]. Returns [SettingViability.Unviable] if
     * it does.
     */
    public fun setOutputIfViable(setting: T): SettingViability

}

/**
 * Throws exception if not viable.
 */
public fun <T : DaqcData> ControlChannel<T>.setOutput(setting: T) {
    setOutputIfViable(setting).throwIfUnviable()
}

// Possible other name confirmSetOutputIfViable.
/**
 * Sets the output to the specified setting and suspends until the setting is set by the system.
 * If the channel is already set to this setting this function will return immediately.
 *
 * Since output settings are conflated it's possible the target setting will never be reached if a different setting
 * is set immediately following this one.
 */
public suspend fun <T : DaqcData> ControlChannel<T>.awaitSetOutputIfViable(
    setting: T,
    timeout: Duration = 1.minutes
): SettingViability = withTimeout(timeout.toLongMilliseconds()) {
    val subscription = openSubscription()
    val viability =  async { setOutputIfViable(setting) }
    subscription.consumingOnEach {
        if (it.value == setting) {
            return@withTimeout viability.await()
        }
    }
    throw IllegalStateException("Should be unreachable due to timeout exception being thrown.")
}

public suspend fun <T : DaqcData> ControlChannel<T>.awaitSetOutput(
    setting: T,
    timeout: Duration = 1.minutes
) {
    awaitSetOutputIfViable(setting, timeout).throwIfUnviable()
}