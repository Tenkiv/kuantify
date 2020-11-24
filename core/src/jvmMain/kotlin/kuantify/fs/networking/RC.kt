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

package kuantify.fs.networking

import org.tenkiv.coral.*

public object RC {
    public const val HTTP: String = "http://"
    public const val DEFAULT_PORT: UInt16 = 8080u

    // HTTP routes
    public const val WEBSOCKET: String = "/ws"
    public const val INFO: String = "/info"

    public const val DEFAULT_HIGH_LOAD_BUFFER: UInt32 = 64u

    // Communicator routes
    public const val DAQC_GATE: String = "daqc_gate"
    public const val FINALIZE: String = "finalize"
    public const val IS_TRANSCEIVING: String = "is_transceiving"
    public const val VALUE: String = "value"
    public const val CONTROL_SETTING: String = "control_setting"
    public const val BUFFER: String = "buffer"
    public const val MAX_ACCEPTABLE_ERROR: String = "max_acceptable_error"
    public const val MAX_VOLTAGE: String = "max_electric_potential"
    public const val BIN_STATE_VALUE: String = "binary_state_value"
    public const val BIN_STATE_CONTROL_SETTING: String = "binary_state_control_setting"
    public const val TRANSITION_FREQUENCY_VALUE: String = "transition_frequency_value"
    public const val TRANSITION_FREQUENCY_CONTROL_SETTING: String = "transition_frequency_control_setting"
    public const val PWM_VALUE: String = "pwm_value"
    public const val PWM_CONTROL_SETTING: String = "pwm_setting"
    public const val IS_TRANSCEIVING_BIN_STATE: String = "is_transceiving_binary_state"
    public const val IS_TRANSCEIVING_PWM: String = "is_transceiving_pwm"
    public const val IS_TRANSCEIVING_FREQUENCY: String = "is_transceiving_frequency"
    public const val START_SAMPLING: String = "start_sampling"
    public const val START_SAMPLING_BINARY_STATE: String = "start_sampling_binary_state"
    public const val START_SAMPLING_PWM: String = "start_sampling_pwm"
    public const val START_SAMPLING_TRANSITION_FREQUENCY: String = "start_sampling_transition_frequency"
    public const val STOP_TRANSCEIVING: String = "stop_transceiving"
    public const val UPDATE_RATE: String = "update_rate"
    public const val PULSE_WIDTH_MODULATE: String = "pulse_width_modulate"
    public const val SUSTAIN_TRANSITION_FREQUENCY: String = "sustain_transition_frequency"
    public const val AVG_PERIOD: String = "avg_period"
    public const val FAILURE: String = "failure"

    public const val CRITICAL_ERROR: String = "critical_error"

    public const val MESSAGE_ERROR: String = "message_error"

}