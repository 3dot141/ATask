package com.yes365.atask.util

import java.util.UUID

actual fun randomUuid(): String = UUID.randomUUID().toString()
