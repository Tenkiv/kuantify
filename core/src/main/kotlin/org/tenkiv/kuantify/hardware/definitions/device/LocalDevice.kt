package org.tenkiv.kuantify.hardware.definitions.device

interface LocalDevice : Device {

    val isHosting: Boolean

    fun startHosting()

    fun stopHosting()

}