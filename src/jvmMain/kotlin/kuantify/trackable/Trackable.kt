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

package kuantify.trackable

import kotlinx.coroutines.flow.*
import physikal.*

public typealias TrackableQuantity<QT> = Trackable<Quantity<QT>>

/*
The type / value of Trackable is not allowed to be nullable. This is because we use null to represent an
uninitialized value. Trackable is intended to be used to represent properties / settings of a device. There is no use
case I can think of where one of these settings will be optional. It may not be set explicitly but it is still there
and should be reported as what it is. Nullable types are the idiomatic way to represent optional fields in Kotlin and
so nullable types have all the nice language features of quickly doing things based on whether the optional value exists
or not. So, since there is basically no good reason to have a nullable Trackable value in the intended use case of
Trackables and valueOrNull may checked fairly often in some cases we want to utilize the conciseness that comes with
nullable types as opposed to making our own sealed class to represent initialized or uninitialized.
*/
/**
 * An object whose value may be continually changing and tracked in a thread safe way.
 */
public interface Trackable<out T : Any> {
    /**
     * The current value of this Trackable or null if the value is unknown because this Trackables value hasn't been
     * initialized. Once initialized, the value can never be null again.
     */
    public val valueOrNull: T?

    /**
     * The flow of all updates to this Trackables value. This flow conflates the value (the flow has no extra buffer and
     * replaces old values with new ones) so it is possible for values set in rapid succession to be skipped and only
     * the most recently set value to be reported. The most recently set value will always be reported which means the
     * flows replay cache size will be exactly 1.
     */
    public val flow: SharedFlow<T>
}

public suspend inline fun <T : Any> Trackable<T>.onEachUpdate(crossinline action: suspend (update: T) -> Unit) {
    flow.collect(action)
}

/**
 * Gets the current value or suspends and waits for one to exist.
 *
 * @return The current value.
 */
public suspend fun <T : Any> Trackable<T>.get(): T = valueOrNull ?: flow.first()