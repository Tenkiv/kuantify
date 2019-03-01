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

package org.tenkiv.kuantify.fs.networking

object RC {
    const val HTTP = "http://"
    const val DEFAULT_PORT = 8080

    //HTTP routes
    const val WEBSOCKET = "/ws"
    const val INFO = "/info"

    // Websocket routes
    const val DAQC_GATE = "daqc_gate"
    const val BUFFER = "buffer"
    const val MAX_ACCEPTABLE_ERROR = "max_acceptable_error"
    const val MAX_ELECTRIC_POTENTIAL = "max_electric_potential"
    const val IS_TRANSCEIVING = "is_transceiving"
    const val IS_TRANSCEIVING_BIN_STATE = "is_transceiving_binary_state"
    const val IS_TRANSCEIVING_PWM = "is_transceiving_pwm"
    const val IS_TRANSCEIVING_FREQUENCY = "is_transceiving_frequency"
    const val VALUE = "value"
    const val START_SAMPLING = "start_sampling"
    const val START_SAMPLING_BINARY_STATE = "start_sampling_binary_state"
    const val START_SAMPLING_PWM = "start_sampling_pwm"
    const val START_SAMPLING_TRANSITION_FREQUENCY = "start_sampling_transition_frequency"
    const val STOP_TRANSCEIVING = "stop_transceiving"
    const val UPDATE_RATE = "update_rate"
    const val PULSE_WIDTH_MODULATE = "pulse_width_modulate"
    const val SUSTAIN_TRANSITION_FREQUENCY = "sustain_transition_frequency"
    const val AVG_FREQUENCY = "avg_frequency"
    const val FAILURE = "failure"

    const val CRITICAL_ERROR = "critical_error"

    const val MESSAGE_ERROR = "message_error"

}