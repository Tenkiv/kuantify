package org.tenkiv.kuantify.gate.control.output

import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.hardware.definitions.device.*
import kotlin.coroutines.*

abstract class KuanitfyRemoteOutput<T : DaqcValue>(val device: RemoteKuantifyDevice) : Output<T> {
    override val coroutineContext: CoroutineContext
        get() = device.coroutineContext

    internal val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<T>>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<T>>
        get() = _updateBroadcaster

    override fun setOutput(setting: T, panicOnFailure: Boolean): SettingResult.Success {
        _updateBroadcaster.offer(setting.now())
        return SettingResult.Success
    }

    internal val _isTransceiving = Updatable(false)
    override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    internal val stopTransceivingChannel = Channel<Unit>(Channel.CONFLATED)
    override fun stopTransceiving() {
        stopTransceivingChannel.offer(Unit)
    }

}