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
import android.animation.ValueAnimator.*
import android.app.*
import android.content.*
import android.os.*
import android.view.animation.*
import android.widget.*
import androidx.databinding.*
import kotlinx.android.synthetic.main.main_layout.*
import java.net.*

class MainActivity : Activity() {
    val va = ValueAnimator.ofFloat(0f, 10f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main_layout)

        ipTextView.text = getLocalIpAddress()

        //create intent that contains the service class
        val serviceIntent = Intent(this, HostService::class.java)

        //initializes service with service's onCreate and onStartCommand
        startService(serviceIntent)
        connectionStatusTextView.text = "connected"

        HostService.isRunning.addOnPropertyChangedCallback(
            object : Observable.OnPropertyChangedCallback() {
                override fun onPropertyChanged(observable: Observable, propertyId: Int) {
                    val observableBool = observable as ObservableBoolean
                    println("Observable boolean value changed to ${observableBool.get()}")
                    if (observableBool.get()) {
                        //service is NOW running
                        setActiveState(statusButton, false)
                    } else {
                        //service is no longer running
                        setActiveState(statusButton, true)
                    }
                }
            }
        )

        statusButton.setOnClickListener {
            if (HostService.isRunning.get()) {
                stopService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
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

    private fun setActiveState(button: Button, isHosting: Boolean) {
        if (isHosting) {
            button.setBackgroundColor(getColor(R.color.primaryAccent))
            button.setTextColor(getColor(R.color.primaryDark))
            button.text = "Start"
            connectionStatusTextView.text = "not hosting"
        } else {
            button.setBackgroundColor(getColor(R.color.primaryInactive))
            button.setTextColor(getColor(R.color.primaryFont))
            button.text = "Stop"
            connectionStatusTextView.text = "hosting"
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.getHostAddress()
                    }
                }
            }
        } catch (ex: SocketException) {
            return "Unknown IP"
        }
        return null
    }
}














