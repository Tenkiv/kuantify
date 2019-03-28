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

package org.tenkiv.kuantify.android.host

import android.app.*
import android.content.*
import android.content.ContentValues.TAG
import android.graphics.*
import android.net.wifi.*
import android.os.*
import android.support.constraint.ConstraintLayout.LayoutParams.PARENT_ID
import android.util.*
import android.view.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.jetbrains.anko.*
import org.jetbrains.anko.constraint.layout.*
import org.jetbrains.anko.constraint.layout.ConstraintSetBuilder.Side.*
import org.tenkiv.kuantify.android.device.*
import org.tenkiv.kuantify.fs.networking.*
import org.tenkiv.kuantify.fs.networking.server.*




//class MainActivity : Activity() {
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
////        setContentView(R.layout.activity_main)
////        val textView = findViewById<TextView>(R.id.textView)
//

//
//        logger.trace { "This is logging of - kotlin-logging" }
//    }
//
//    companion object : KLogging()
//}

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "Starting Activity")
        MainActivityUI(applicationContext).setContentView(this)
    }
}

class MainActivityUI(private val applicationContext: Context) : AnkoComponent<MainActivity> {
    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {
        val bgColor = Color.parseColor("#142128")
        val fontColor = Color.parseColor("#a3b3b7")
        val kuantifyColor = Color.parseColor("#9b96e2")

        val server = embeddedServer(Netty, port = RC.DEFAULT_PORT) {
            kuantifyHost()
        }
        server.start()

        val device = LocalAndroidDevice.get(applicationContext)
        device.startHosting()

        constraintLayout {
            setBackgroundColor(bgColor)

            val title = textView("Kuantify Host") {
                id = View.generateViewId()
                textSize = 48f
                textColor = fontColor
            }.lparams(width = matchParent) {
                setPadding(12, 0, 12, 0)
            }

            val wifiMan = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInf = wifiMan.connectionInfo
            val ipAddress = wifiInf.ipAddress
            val ip = String.format(
                "%d.%d.%d.%d", ipAddress and 0xff, ipAddress shr 8 and 0xff, ipAddress shr 16 and 0xff,
                ipAddress shr 24 and 0xff
            )

            val ipText = textView(ip) {
                id = View.generateViewId()
                textSize = 24f
                textColor = kuantifyColor
            }.lparams(width = matchParent) {
                setPadding(12, 0, 12, 0)
            }

            val startButton = button("Start") {
                id = View.generateViewId()
                textSize = 48f
                setBackgroundColor(kuantifyColor)
                textColor = Color.BLACK
            }.lparams {
                setPadding(12, 0, 12, 0)
            }

            val endButton = button("Close") {
                id = View.generateViewId()
                textSize = 48f
                setBackgroundColor(Color.DKGRAY)
                textColor = fontColor
            }.lparams {
                setPadding(12, 0, 12, 0)
            }

            applyConstraintSet {
                connect(
                    TOP of title to TOP of PARENT_ID margin dip(40),
                    TOP of ipText to BOTTOM of title margin dip(40),
                    TOP of startButton to BOTTOM of ipText margin dip(50),
                    TOP of endButton to BOTTOM of startButton margin dip(20)
                )
            }
        }
    }
}