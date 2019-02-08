package org.tenkiv.kuantify.android.host

import android.app.*
import android.os.*
import android.widget.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.tenkiv.kuantify.networking.server.*
import org.tenkiv.kuantify.simple_host.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val textView = findViewById<TextView>(R.id.textView)

        val server = embeddedServer(Netty, port = 80) {
            kuantifyHost()
        }
        server.start()

    }

}