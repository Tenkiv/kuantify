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

package org.tenkiv.kuantify.gate.control.output

/**
 * Kuantify handles ensuring delivery of commands and therefore assumes that commands were actually carried out by
 * the daqc device. So Success is not a 100% guarantee that the output was actually set to this. However, if [Success]
 * is returned and there is a failure to set the output in the communication process, the error will be propagated
 * through the daqcCriticalErrorChannel.
 *
 * setOutput() can still throw exceptions separately from the [SettingResult] pipeline in the event of unexpected
 * catastrophic problems in the setting process.
 */
sealed class SettingResult {

    object Success : SettingResult()

    class Failure(val exception: SettingException, panic: Boolean) : SettingResult() {

        init {
            if (panic) throw exception
        }

    }

}

open class SettingException(val output: Output<*>, override val message: String, cause: Throwable? = null) :
    Throwable("$output: $message", cause)

class UninitialisedSettingException(output: Output<*>, cause: Throwable? = null) :
    SettingException(output, message, cause) {

    companion object {
        private const val message = "attempted to modify uninitialised setting"
    }
}

class SettingOutOfRangeException(output: Output<*>, cause: Throwable? = null) :
    SettingException(output, message, cause) {

    companion object {
        private const val message = "attempted setting is out of the allowable range"
    }
}