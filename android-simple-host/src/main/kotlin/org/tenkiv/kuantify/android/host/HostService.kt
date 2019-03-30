///*
// * Copyright 2019 Tenkiv, Inc.
// *
// * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
// * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
// * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
// * permit persons to whom the Software is furnished to do so, subject to the following conditions:
// *
// * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
// * Software.
// *
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
// * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
// * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
// * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
// */
//
//package org.tenkiv.kuantify.android.host
//
//import android.app.*
//import android.content.*
//import android.os.*
//import android.support.v4.app.*
//import android.support.v4.content.*
//import io.ktor.server.engine.*
//import io.ktor.server.netty.*
//import kotlinx.coroutines.*
//import org.tenkiv.kuantify.android.device.*
//import org.tenkiv.kuantify.fs.networking.*
//import org.tenkiv.kuantify.fs.networking.server.*
//import java.util.concurrent.*
//
////val server = embeddedServer(Netty, port = RC.DEFAULT_PORT) { kuantifyHost() }
////val device = LocalAndroidDevice.get(applicationContext)
//
//class HostService : Service() {
//
//    //called only once when the service is first initialized
//    override fun onCreate() {
//        super.onCreate()
//
//        val showActivityPI = PendingIntent.getActivity(
//            this,
//            0,
//            Intent(this, MainActivity::class.java),
//            0
//        )
//
//        val stopServiceIntent = Intent(this, ActionReceiver::class.java)
//
//        val lbm = LocalBroadcastManager.getInstance(this).registerReceiver()
//
//        val stopServicePI = PendingIntent.getBroadcast(
//            this,
//            1,
//            stopServiceIntent,
//            0
//        )
//
//        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
//            .setContentTitle("Kuantify Host Service")
//            .setContentText("Kuantify is hosting device")
//            .setSmallIcon(R.drawable.ic_daqc_dude_notification_icon)
//            .setContentIntent(showActivityPI)
//            .addAction(R.drawable.ic_daqc_dude_notification_icon, "Stop Hosting", stopServicePI)
//            .build()
//
//        server.start()
//        device.startHosting()
//
//        startForeground(1, notification)
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        return super.onStartCommand(intent, flags, startId)
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//    }
//}
//
//class ActionReceiver : BroadcastReceiver() {
//    override fun onReceive(context: Context?, intent: Intent?) {
//        GlobalScope.launch {
//            device.stopHosting()
//            server.stop(1, 5, TimeUnit.SECONDS)
//        }
//    }
//}