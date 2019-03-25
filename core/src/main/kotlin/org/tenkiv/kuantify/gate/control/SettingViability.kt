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

package org.tenkiv.kuantify.gate.control

import mu.*
import org.tenkiv.kuantify.gate.control.SettingViability.*

private val logger = KotlinLogging.logger {}

/**
 * Kuantify handles ensuring delivery of commands and therefore assumes that commands were actually carried out by
 * the daqc device. So Viable is not a 100% guarantee that the output was actually set to this. However, if [Viable]
 * is returned and there is a failure to set the output in the communication process, the error will be propagated
 * through the daqcCriticalErrorChannel.
 *
 * setOutput() can still throw exceptions separately from the [SettingViability] pipeline in the event of unexpected
 * catastrophic problems in the setting process.
 */
public sealed class SettingViability {

    public fun panicIfUnviable() {
        if (this is Unviable) panic()
    }

    public object Viable : SettingViability()

    public class Unviable(val exception: SettingException) : SettingViability() {

        init {
            logger.debug {
                "Attempted unviable setting for ControlGate: ${exception.controlGate}. ${exception.message}"
            }
        }

        public fun panic() {
            throw exception
        }

    }

}

public open class SettingException(val controlGate: ControlGate<*>, message: String, cause: Throwable? = null) :
    Throwable("$controlGate: $message", cause)

public class UninitialisedSettingException(controlGate: ControlGate<*>, cause: Throwable? = null) :
    SettingException(controlGate, message, cause) {

    public companion object {
        private const val message = "Attempted to modify uninitialised setting."
    }
}

public class SettingOutOfRangeException(controlGate: ControlGate<*>, cause: Throwable? = null) :
    SettingException(controlGate, message, cause) {

    public companion object {
        private const val message = "Attempted setting is out of the allowable range."
    }
}

public class ConnectionException(controlGate: ControlGate<*>, cause: Throwable? = null) :
    SettingException(controlGate, message, cause) {

    public companion object {
        private const val message = "There is no connection to the device to which this control gate belongs."
    }
}