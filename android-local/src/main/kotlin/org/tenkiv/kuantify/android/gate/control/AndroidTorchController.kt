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

package org.tenkiv.kuantify.android.gate.control

import android.hardware.camera2.*
import kotlinx.coroutines.channels.*
import org.tenkiv.coral.*
import org.tenkiv.kuantify.*
import org.tenkiv.kuantify.android.device.*
import org.tenkiv.kuantify.data.*
import org.tenkiv.kuantify.fs.gate.control.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.gate.control.*
import org.tenkiv.kuantify.networking.configuration.*
import java.util.concurrent.atomic.*
import kotlin.coroutines.*

class AndroidTorchController(val device: LocalAndroidDevice, override val uid: String, val cameraId: String) :
    AndroidOutput<BinaryState>, LocalOutput<BinaryState> {

    override val coroutineContext: CoroutineContext
        get() = device.coroutineContext

    override val basePath: Path = listOf(RC.DAQC_GATE, uid)

    private val torchCallbackRegistered = AtomicBoolean(false)

    private val _updateBroadcaster = ConflatedBroadcastChannel<ValueInstant<BinaryState>>()
    override val updateBroadcaster: ConflatedBroadcastChannel<out ValueInstant<BinaryState>>
        get() = _updateBroadcaster

    private val _isTransceiving = Updatable(false)
    override val isTransceiving: InitializedTrackable<Boolean>
        get() = _isTransceiving

    private val torchCallback: CameraManager.TorchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)

            if (cameraId == this@AndroidTorchController.cameraId) {
                _updateBroadcaster.offer((if (enabled) BinaryState.High else BinaryState.Low).now())
            }
        }
    }

    //TODO: This may need to take camera device availability into account
    override fun setOutput(setting: BinaryState): SettingViability.Viable {
        if (!torchCallbackRegistered.get()) {
            device.cameraManager.registerTorchCallback(torchCallback, null)
            _isTransceiving.value = true
        }

        device.cameraManager.setTorchMode(cameraId, setting.toBoolean())
        return SettingViability.Viable
    }

    override fun stopTransceiving() {
        device.cameraManager.unregisterTorchCallback(torchCallback)
        _isTransceiving.value = false
    }
}