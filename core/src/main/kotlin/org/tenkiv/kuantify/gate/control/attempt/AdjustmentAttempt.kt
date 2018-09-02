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

import org.tenkiv.kuantify.gate.control.output.SettingException
import kotlin.reflect.KClass

sealed class AdjustmentAttempt : Viability {

    data class OK(override val nextStep: Viability? = null) : AdjustmentAttempt() {
        override val isViable get() = nextStep?.isViable ?: true

        override val parentType: KClass<*>? = null

        override val message get() = nextStep?.message ?: Viability.OK_MESSAGE
    }

    object UninitialisedSetting : AdjustmentAttempt() {
        override val isViable get() = false

        override val nextStep: Viability? = null

        override val parentType: KClass<*>? = null

        override val message
            get() = "Cannot adjust uninitialised setting, set an initial setting before attempting to adjust."

        fun getOrThrow(throwIfNotViable: Boolean): UninitialisedSetting =
            if (throwIfNotViable) panic() else this

        fun panic(): Nothing = throw UninitialisedSettingException()
    }

    class UninitialisedSettingException : SettingException(UninitialisedSetting)
}