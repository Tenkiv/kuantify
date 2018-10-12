package org.tenkiv.kuantify.networking.client.tcpip

import org.tenkiv.kuantify.networking.ConnectionHandler

/*
abstract class RestConnectionHandler: ConnectionHandler {

    */
/*private val client = HttpClient(CIO)

    private val _responseChannel = ConflatedBroadcastChannel<Pair<HttpRequestData,String>>()

    val responseChannel: ConflatedBroadcastChannel<out Pair<HttpRequestData,String>> = _responseChannel

    fun send(method: HttpMethod, body: Any, vararg headers: Pair<String, String>) {
        val builder = HttpRequestBuilder()
        builder.method = method
        builder.body = body
        headers.forEach { builder.header(it.first, it.second) }

        launch {
            _responseChannel.offer(Pair(builder.build(),client.get(builder)))
        }
    }*//*


}*/
