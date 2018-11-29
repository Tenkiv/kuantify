package org.tenkiv.kuantify.networking

import io.kotlintest.specs.StringSpec
import kotlinx.coroutines.io.ByteReadChannel

class ConnectionHandlerTest : StringSpec({
    "Test TCP Connection"{

        val writeChannel = ByteReadChannel("Here is some text\n Here is more text\n Here is even more ext")

        /*launch {
            writeChannel.readSuspendableSession {
                println("Something?")
            }
        }

        delay(20000)*/

        /*val socketAddress = InetSocketAddress("172.217.9.142", 80)

        val tcpConnection =
            TcpSocketHandler(TransportProtocol.Tcp(socketAddress), false)

        tcpConnection.connect()*/

        //println("Socket Read "+String(it.array()))

        Thread.sleep(20000)
    }
})