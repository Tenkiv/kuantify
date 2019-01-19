package org.tenkiv.kuantify.networking

import kotlinx.serialization.*

@Serializable
internal data class NetworkMessage(val path: List<String>, val value: String)