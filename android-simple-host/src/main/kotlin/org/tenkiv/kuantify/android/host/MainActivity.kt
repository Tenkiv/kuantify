package org.tenkiv.kuantify.android.host

import android.app.*
import android.os.*
import android.widget.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.tenkiv.kuantify.android.device.*
import org.tenkiv.kuantify.simple_host.*

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val textView = findViewById<TextView>(R.id.textView)

        val androidDevice = LocalAndroidDevice.get(this)
        GlobalScope.launch(Dispatchers.Main) {
            androidDevice.lightSensors.first().startSampling()
            androidDevice.lightSensors.first().updateBroadcaster.consumeEach {
                textView.text = it.toString()
            }
        }
    }

}