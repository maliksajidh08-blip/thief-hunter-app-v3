package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Live Tracking Screen backed by OpenStreetMap WebView
 * URL: https://maliksajidh08-blip.github.io/thief-hunter-website/track.html
 * Integrates real Android GPS telemetry via WebAppInterface JavaScript bridge.
 */
@Composable
fun LiveTrackingScreen(
    viewModel: ThiefHunterViewModel,
    modifier: Modifier = Modifier
) {
    WebViewTrackingScreen(
        viewModel = viewModel,
        modifier = modifier
    )
}
