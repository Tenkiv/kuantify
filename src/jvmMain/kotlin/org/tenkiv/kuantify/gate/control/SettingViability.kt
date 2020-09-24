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

import mu.*
import org.tenkiv.kuantify.gate.control.SettingViability.*

private val logger = KotlinLogging.logger {}

/**
 * Kuantify handles ensuring delivery of commands and therefore assumes that commands were actually carried out by
 * the daqc device. So Viable is not a 100% guarantee that the output was actually set to this. However, if [Viable]
 * is returned and there is a failure to set the output in the communication process, the error will be propagated
 * through the daqcCriticalErrorChannel.
 */
public sealed class SettingViability {

    public fun throwIfUnviable() {
        if (this is Unviable) throwException()
    }

    public object Viable : SettingViability()

    public class Unviable(public val problem: SettingProblem) : SettingViability() {

        init {
            logger.warn {
                "Attempted unviable setting for ControlGate: ${problem.controlGate}. ${problem.message}"
            }
        }

        public fun throwException(): Nothing = throw UnviableSettingException(problem)

    }

}

public sealed class SettingProblem {
    public abstract val controlGate: ControlChannel<*>
    public abstract val message: String
    public abstract val cause: Throwable?

    public class UninitialisedSetting internal constructor(
        public override val controlGate: ControlChannel<*>,
        public override val cause: Throwable? = null
    ) : SettingProblem() {
        public override val message: String = "Attempted to modify uninitialised setting."
    }

    public class OutOfRange internal constructor(
        public override val controlGate: ControlChannel<*>,
        public override val cause: Throwable? = null
    ) : SettingProblem() {
        public override val message: String = "Attempted setting is out of the allowable range."
    }

}

public fun ControlChannel<*>.UninitialisedSetting(cause: Throwable? = null): Unviable =
    Unviable(SettingProblem.UninitialisedSetting(this, cause))

public fun ControlChannel<*>.SettingOutOfRange(cause: Throwable? = null): Unviable =
    Unviable(SettingProblem.OutOfRange(this, cause))

public class UnviableSettingException(problem: SettingProblem) : Exception(problem.message, problem.cause)