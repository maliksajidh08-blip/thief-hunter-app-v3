package com.example.ui

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.widget.Toast

/**
 * JavaScript Bridge for Thief Hunter OpenStreetMap tracking WebView.
 * Exposes methods to web client under `window.Android.*`
 */
class WebAppInterface(
    private val context: Context,
    private val getCurrentLocation: () -> Pair<Double, Double>?,
    private val onLocationReceivedFromWeb: ((Double, Double) -> Unit)? = null
) {
    /**
     * Called by website JavaScript:
     * window.Android.getAndroidLocation()
     * Returns comma-separated coordinates: "$lat,$lng"
     */
    @JavascriptInterface
    fun getAndroidLocation(): String {
        val loc = getCurrentLocation()
        return if (loc != null && (loc.first != 0.0 || loc.second != 0.0)) {
            "${loc.first},${loc.second}"
        } else {
            ""
        }
    }

    /**
     * Called by website JavaScript:
     * window.Android.getLatitude()
     */
    @JavascriptInterface
    fun getLatitude(): Double {
        return getCurrentLocation()?.first ?: 0.0
    }

    /**
     * Called by website JavaScript:
     * window.Android.getLongitude()
     */
    @JavascriptInterface
    fun getLongitude(): Double {
        return getCurrentLocation()?.second ?: 0.0
    }

    /**
     * Called by website JavaScript when coordinates are updated on map:
     * window.Android.updateLocation(lat, lng)
     */
    @JavascriptInterface
    fun updateLocation(lat: Double, lng: Double) {
        Handler(Looper.getMainLooper()).post {
            onLocationReceivedFromWeb?.invoke(lat, lng)
        }
    }

    /**
     * Toast helper for debugging and web alerts
     */
    @JavascriptInterface
    fun showToast(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
