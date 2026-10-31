package com.yes365.atask.auth

import java.util.UUID

actual fun generateStateValue(): String {
    return UUID.randomUUID().toString()
}
