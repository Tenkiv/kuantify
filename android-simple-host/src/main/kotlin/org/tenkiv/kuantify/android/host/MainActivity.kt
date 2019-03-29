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
 */

package org.tenkiv.kuantify.android.host

import android.app.*
import android.os.*
import android.view.animation.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.android.synthetic.main.main_layout.*
import org.tenkiv.kuantify.android.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.fs.networking.server.*

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val server = embeddedServer(Netty, port = RC.DEFAULT_PORT) {
            kuantifyHost()
        }
        server.start()

        val device = LocalAndroidDevice.get(applicationContext)
        device.startHosting()
        setContentView(R.layout.main_layout)

        val daqcDudeAnimation = TranslateAnimation(0f, 0f, 0f, 10f).apply {
            interpolator = AccelerateDecelerateInterpolator()
            duration = 2000
            fillAfter = false
            repeatCount = Animation.INFINITE
        }

        daqcDudeImageView.startAnimation(daqcDudeAnimation)
    }
}












