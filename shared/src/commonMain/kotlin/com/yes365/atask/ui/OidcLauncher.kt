package com.yes365.atask.ui

import androidx.compose.runtime.Composable
import com.yes365.atask.auth.OidcHandler

@Composable
expect fun rememberOidcLauncher(
    onResult: (code: String?, state: String?, error: String?) -> Unit
): (OidcHandler.AuthParams) -> Unit
