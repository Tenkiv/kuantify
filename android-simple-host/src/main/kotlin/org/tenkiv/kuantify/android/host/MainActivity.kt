package org.tenkiv.kuantify.android.host

import android.app.*
import android.os.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import org.tenkiv.kuantify.android.device.*
import org.tenkiv.kuantify.networking.server.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        setContentView(R.layout.activity_main)
//        val textView = findViewById<TextView>(R.id.textView)

        val server = embeddedServer(Netty, port = 8080) {
            kuantifyHost()
        }
        server.start()

        println("GlobalScope: ${GlobalScope.coroutineContext}")

        val device = LocalAndroidDevice.get(this)
        device.startHosting()
    }

}