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

package kuantify

import kuantify.gate.*

public interface Finalizable {
    /**
     * Backed by atomic.
     */
    public val isFinalized: Boolean

    /**
     * Finalize the configuration of this [Finalizable] so nothing can be changed for the remainder of its existence.
     * This will also finalize any [Finalizable]s this [Finalizable]s data is derived from and all [Finalizable]s it
     * hosts.
     *
     * Attempts to modify the configuration of a [Finalizable] after [finalize] is called will fail.
     *
     * This is an idempotent operation - subsequent calls to this function have no effect.
     */
    public fun finalize()
}

/**
 * Convenience function to wrap [DaqcChannel] configuration modification calls in for automatic handling of
 * [DaqcChannel.isFinalized]. This function should only be use when creating a new type of [DaqcChannel].
 */
@KuantifyComponentBuilder
public inline fun <R> Finalizable.modifyConfiguration(block: () -> R): R = if (!isFinalized) {
    block()
} else {
    throw IllegalStateException("Cannot modify configuration of DaqcGate that has been finalized.")
}

public fun Iterable<Finalizable>.finalizeAll() {
    forEach { it.finalize() }
}

public fun Array<out Finalizable>.finalizeAll() {
    forEach { it.finalize() }
}
