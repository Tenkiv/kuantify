package org.tenkiv.kuantify.networking.client

import io.ktor.client.*
import io.ktor.client.features.websocket.*

internal val httpClient = HttpClient {
    install(WebSockets)
}