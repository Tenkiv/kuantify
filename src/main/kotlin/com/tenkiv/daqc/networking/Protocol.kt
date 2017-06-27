package com.tenkiv.daqc.networking

/**
 * Created by tenkiv on 4/7/17.
 */
enum class NetworkProtocol {
    UDP,
    TCP,
    SSH,
    TELNET,
    COUCHBASE

}

enum class SharingStatus {
    NONE,
    READ_ALL,
    READ_WRITE_ALL,
    CUSTOM
}

class UnsupportedProtocolException : Exception("Board unable to connect with supplied protocol.")