package com.yes365.atask.auth

import com.yes365.atask.util.randomUuid

actual fun generateStateValue(): String = randomUuid()
