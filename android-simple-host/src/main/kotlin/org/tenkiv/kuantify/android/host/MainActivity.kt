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

import android.animation.*
import android.animation.ValueAnimator.INFINITE
import android.animation.ValueAnimator.REVERSE
import android.app.*
import android.content.*
import android.net.wifi.*
import android.os.*
import android.view.animation.*
import android.widget.*
import kotlinx.android.synthetic.main.main_layout.*
import kotlinx.coroutines.*
import org.tenkiv.kuantify.android.device.*



class MainActivity : Activity() {
    val va = ValueAnimator.ofFloat(0f, 10f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_layout)

//        val server = embeddedServer(Netty, port = RC.DEFAULT_PORT) { kuantifyHost() }
//        server.start()
//        val device = LocalAndroidDevice.get(applicationContext)
//        startHosting(device)

        val wm = getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInf = wm.connectionInfo
        val ipAddress = wifiInf.ipAddress
        println(ipAddress)

        //create intent that contains the service class
        val serviceIntent = Intent(this, HostService::class.java)

        //initializes service with service's onCreate and onStartCommand
        startService(serviceIntent)


        startButton.setOnClickListener { startButton ->
            startService(serviceIntent)

            setActiveState(startButton as Button, false)
            setActiveState(stopButton, true)

        }

        stopButton.setOnClickListener { stopButton ->
            stopService(serviceIntent)

            setActiveState(stopButton as Button, false)
            setActiveState(startButton, true)
        }

        //animation
        va.apply {
            duration = 1000
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                daqcDudeImageView.translationY = it.animatedValue as Float
            }
            repeatCount = INFINITE
            repeatMode = REVERSE
            start()
        }
    }

    override fun onPause() {
        super.onPause()
        va.pause()
    }

    override fun onResume() {
        super.onResume()
        va.resume()
    }

    private fun setActiveState(button: Button, setActive: Boolean) {
        if (setActive) {
            button.setBackgroundColor(getColor(R.color.primaryAccent))
            button.setTextColor(getColor(R.color.primaryDark))
        } else {
            button.setBackgroundColor(getColor(R.color.primaryInactive))
            button.setTextColor(getColor(R.color.primaryFont))
        }
    }

    private fun startHosting(device: LocalAndroidDevice) {
        device.startHosting()
        connectionStatusTextView.text = "connected"
    }

    private fun stopHosting(device: LocalAndroidDevice) {
        GlobalScope.launch {
            device.stopHosting()
        }
        connectionStatusTextView.text = "disconnected"
    }
}














