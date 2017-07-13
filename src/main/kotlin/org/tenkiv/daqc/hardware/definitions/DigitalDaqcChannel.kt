package org.tenkiv.daqc.hardware.definitions

interface DigitalDaqcChannel : DaqcChannel {

    val isActive get() = isActiveForBinaryState || isActiveForPwm || isActiveForTransitionFrequency

    val isActiveForBinaryState: Boolean

    val isActiveForPwm: Boolean

    val isActiveForTransitionFrequency: Boolean

}