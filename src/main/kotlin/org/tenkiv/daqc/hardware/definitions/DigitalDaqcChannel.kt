package org.tenkiv.daqc.hardware.definitions

interface DigitalDaqcChannel : DaqcChannel {

    val isActive get() = isActiveForBinaryState || isActiveForPwm || isActiveForTransitionFrequency

    /**
     * Implementing backing  field must be atomic or otherwise provide safety for
     * reads from multiple threads.
     */
    val isActiveForBinaryState: Boolean

    /**
     * Implementing backing  field must be atomic or otherwise provide safety for
     * reads from multiple threads.
     */
    val isActiveForPwm: Boolean

    /**
     * Implementing backing  field must be atomic or otherwise provide safety for
     * reads from multiple threads.
     */
    val isActiveForTransitionFrequency: Boolean

}