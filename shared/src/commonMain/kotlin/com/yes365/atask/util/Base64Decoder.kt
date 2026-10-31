package com.yes365.atask.util

expect object Base64Decoder {
    fun decodeUrlSafe(src: String): ByteArray
}
