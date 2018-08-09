package org.tenkiv.daqc.networking

enum class NetworkProtocol {
    UDP,
    TCP,
    SSH,
    TELNET
}

enum class SharingStatus {
    NONE,
    READ_ALL,
    READ_WRITE_ALL
}

class UnsupportedProtocolException : Throwable("Board unable to connect with supplied protocol.")