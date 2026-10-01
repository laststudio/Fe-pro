package com.shuaiqiu.fuckets100

import java.security.MessageDigest

internal fun sha256LowerHex(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes)
    .joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xff) }

internal fun sha256LowerHex(value: String): String = sha256LowerHex(value.toByteArray(Charsets.UTF_8))
