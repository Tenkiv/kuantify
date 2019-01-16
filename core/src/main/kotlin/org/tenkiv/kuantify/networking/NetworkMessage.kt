package org.tenkiv.kuantify.networking

import kotlinx.serialization.*

@Serializable
internal data class NetworkMessage<T : Any>(val path: List<String>, val value: T)