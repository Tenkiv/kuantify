package org.tenkiv.kuantify.networking

import kotlinx.serialization.*

@Serializable
internal data class NetworkMessage(val route: List<String>, @Optional val value: String? = null)