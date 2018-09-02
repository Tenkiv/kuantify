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

package org.tenkiv.kuantify.gate.control.attempt

/**
 * @property parent The base level [Viability] object that corresponds to this one. This exists so final classes can
 * return a sealed class for setOutput but the Viability can still be checked for the base type. For example you might
 * have TekdaqcAnalogOutput return TekdaqcAnalogOutput.Attempt.OutOfRange and have the parent be a corresponding
 * instance of [RangeLimitedAttempt.OutOfRange]
 * @property isViable is only meant to represent if the setting can succeed in the current situation, not whether or not
 * the implementation of the setting was successful.
 */
interface Viability {
    val isViable: Boolean

    val nextStep: Viability?

    val parent: Viability?

    val message: String

    companion object {
        const val OK_MESSAGE = "OK"
    }
}

inline fun <reified T : Viability> Viability.getResultOfType(): T? {
    var viability: Viability? = this
    while (viability != null) {
        if (viability is T) return viability

        val parent = viability.parent
        if (parent is T) return parent

        viability = viability.nextStep
    }

    return null
}

inline fun <reified T : Viability> Viability.containsResultOfType(): Boolean = getResultOfType<T>() != null

object Viable : Viability {
    override val isViable get() = true

    override val nextStep: Viability? = null

    override val parent: Viability? = null

    override val message get() = Viability.OK_MESSAGE
}
