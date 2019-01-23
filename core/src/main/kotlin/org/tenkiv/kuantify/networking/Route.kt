package org.tenkiv.kuantify.networking

object Route {
    const val DEVICE = "device"

    const val DAQC_GATE = "gaqc_gate"
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

    const val CRITICAL_ERROR = "critical_error"

    const val MESSAGE_ERROR = "message_error"

}