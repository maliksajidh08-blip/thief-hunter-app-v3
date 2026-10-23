package com.example.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.webkit.GeolocationPermissions
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.LocationHistoryEntity
import com.example.ui.theme.AlertRed
import com.example.ui.theme.NavyCardDark
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.SafeGreen
import com.example.ui.theme.YellowAccent
import kotlinx.coroutines.delay
import java.util.Locale

private const val TRACKING_URL = "https://maliksajidh08-blip.github.io/thief-hunter-website/track.html"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewTrackingScreen(
    viewModel: ThiefHunterViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val tracker = state.liveTrackerState
    val context = LocalContext.current

    // Real GPS coordinates priority: Tracker State -> Last Fix -> Telemetry
    val actualLat = when {
        tracker.latitude != 0.0 -> tracker.latitude
        state.lastKnownLocation != null && state.lastKnownLocation!!.latitude != 0.0 -> state.lastKnownLocation!!.latitude
        state.sensorTelemetry.latitude != 0.0 -> state.sensorTelemetry.latitude
        else -> 0.0
    }
    val actualLng = when {
        tracker.longitude != 0.0 -> tracker.longitude
        state.lastKnownLocation != null && state.lastKnownLocation!!.longitude != 0.0 -> state.lastKnownLocation!!.longitude
        state.sensorTelemetry.longitude != 0.0 -> state.sensorTelemetry.longitude
        else -> 0.0
    }
    val isGpsLocked = actualLat != 0.0 && actualLng != 0.0
    val latStr = if (isGpsLocked) String.format(Locale.US, "%.6f", actualLat) else "Locking..."
    val lngStr = if (isGpsLocked) String.format(Locale.US, "%.6f", actualLng) else "GPS..."
    val mapsUrl = if (isGpsLocked) "https://maps.google.com/?q=$latStr,$lngStr" else "https://maps.google.com"

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var showHistorySheet by remember { mutableStateOf(false) }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val net = cm?.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Helper to send real GPS coordinates to website JavaScript and override OpenStreetMap
    fun pushLocationToWebView(
        wv: WebView?,
        lat: Double,
        lng: Double,
        accuracy: Float = tracker.accuracyMeters,
        battery: Int = state.sensorTelemetry.batteryPct
    ) {
        if (wv == null || (lat == 0.0 && lng == 0.0)) return
        val js = """
            (function() {
                var targetLat = $lat;
                var targetLng = $lng;
                var acc = $accuracy;
                var bat = "$battery%";

                function updateLeaflet() {
                    var theMap = (typeof map !== 'undefined' && map) ? map : window.map;
                    if (!theMap) return false;

                    theMap.setView([targetLat, targetLng], 16);

                    var theMarker = (typeof marker !== 'undefined' && marker) ? marker : window.marker;
                    if (theMarker) {
                        theMarker.setLatLng([targetLat, targetLng]);
                        theMarker.bindPopup(`
                            <div style="font-family: sans-serif; text-align: center; padding: 8px;">
                                <strong style="color: #1A237E; font-size: 16px;">🎯 تھیفٹ ہنٹر</strong><br>
                                <span style="color: #27AE60; font-weight: bold;">🟢 حقیقی GPS فعال (Real GPS)</span><br>
                                <span style="color: #1565C0; font-size: 13px; font-weight: bold;">` + targetLat.toFixed(6) + `, ` + targetLng.toFixed(6) + `</span><br>
                                <span style="color: #6C757D; font-size: 11px;">درستگی: ±` + (acc > 0 ? Math.round(acc) + ' میٹر' : '5 میٹر') + `</span>
                            </div>
                        `).openPopup();
                    } else if (typeof L !== 'undefined') {
                        marker = L.marker([targetLat, targetLng]).addTo(theMap);
                    }

                    if (typeof window.gpsAccuracyCircle !== 'undefined' && window.gpsAccuracyCircle) {
                        theMap.removeLayer(window.gpsAccuracyCircle);
                    }
                    if (typeof L !== 'undefined') {
                        window.gpsAccuracyCircle = L.circle([targetLat, targetLng], {
                            color: '#27AE60',
                            fillColor: '#27AE60',
                            fillOpacity: 0.15,
                            radius: (acc > 0 ? acc : 50)
                        }).addTo(theMap);
                    }

                    var locSpan = document.getElementById('current-location');
                    if (locSpan) {
                        locSpan.textContent = targetLat.toFixed(5) + ', ' + targetLng.toFixed(5) + ' (حقیقی GPS)';
                    }
                    var batSpan = document.getElementById('battery');
                    if (batSpan) {
                        batSpan.textContent = bat;
                    }
                    var netSpan = document.getElementById('network');
                    if (netSpan) {
                        netSpan.textContent = 'GPS Active (Android)';
                    }
                    return true;
                }

                if (!updateLeaflet()) {
                    var retries = 0;
                    var timer = setInterval(function() {
                        retries++;
                        if (updateLeaflet() || retries >= 15) {
                            clearInterval(timer);
                        }
                    }, 300);
                }
            })();
        """.trimIndent()
        wv.evaluateJavascript(js, null)
    }

    // Immediately trigger fresh GPS fix when screen opens
    LaunchedEffect(Unit) {
        viewModel.requestImmediateGpsFix()
    }

    // Reactively push whenever real GPS updates
    LaunchedEffect(actualLat, actualLng, webViewInstance) {
        webViewInstance?.let { wv ->
            pushLocationToWebView(wv, actualLat, actualLng)
        }
    }

    // Periodically (every 15 seconds) refresh real GPS
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000L)
            viewModel.requestImmediateGpsFix()
            webViewInstance?.let { wv ->
                pushLocationToWebView(wv, actualLat, actualLng)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. TOP HEADER & TELEMETRY STATUS BAR
        Surface(
            color = NavyDark,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (state.isLocationTracking30sActive) SafeGreen else Color.Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "OpenStreetMap Live Tracker",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (isOnline()) "Connected to Web Satellite Tiles" else "Offline • Cached Map Active",
                                fontSize = 11.sp,
                                color = if (isOnline()) SafeGreen else YellowAccent
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Toggle History
                        IconButton(
                            onClick = { showHistorySheet = !showHistorySheet },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                tint = if (showHistorySheet) YellowAccent else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Refresh WebView
                        IconButton(
                            onClick = {
                                loadError = null
                                webViewInstance?.reload()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload Map",
                                tint = YellowAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (isLoading) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { loadProgress / 100f },
                        color = YellowAccent,
                        trackColor = NavyPrimary.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth().height(3.dp)
                    )
                }
            }
        }

        // 2. MAIN MAP / WEBVIEW CONTAINER
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("webview_osm_map"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        // Enforce RTL layout direction and text direction for Urdu
                        layoutDirection = android.view.View.LAYOUT_DIRECTION_RTL
                        textDirection = android.view.View.TEXT_DIRECTION_RTL

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            setSupportZoom(true)
                            defaultTextEncodingName = "UTF-8"
                            setGeolocationEnabled(true)
                            databaseEnabled = true
                            cacheMode = if (isOnline()) WebSettings.LOAD_DEFAULT else WebSettings.LOAD_CACHE_ELSE_NETWORK
                            builtInZoomControls = true
                            displayZoomControls = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }

                        // Add Javascript Interface for window.Android.*
                        addJavascriptInterface(
                            WebAppInterface(
                                context = ctx,
                                getCurrentLocation = { Pair(actualLat, actualLng) },
                                onLocationReceivedFromWeb = { lat, lng ->
                                    Toast.makeText(ctx, "Map updated: $lat, $lng", Toast.LENGTH_SHORT).show()
                                }
                            ),
                            "Android"
                        )

                        webChromeClient = object : WebChromeClient() {
                            override fun onGeolocationPermissionsShowPrompt(
                                origin: String?,
                                callback: GeolocationPermissions.Callback?
                            ) {
                                callback?.invoke(origin, true, false)
                            }

                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress
                                if (newProgress >= 100) {
                                    isLoading = false
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                loadError = null
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                view?.evaluateJavascript(
                                    "try { document.documentElement.setAttribute('dir', 'rtl'); document.documentElement.setAttribute('lang', 'ur'); document.body.style.direction = 'rtl'; } catch(e){}",
                                    null
                                )
                                pushLocationToWebView(view, actualLat, actualLng)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                if (request?.isForMainFrame == true) {
                                    isLoading = false
                                    loadError = error?.description?.toString() ?: "Failed to connect to tracking website"
                                }
                            }
                        }

                        loadUrl(TRACKING_URL)
                        webViewInstance = this
                    }
                },
                update = { wv ->
                    webViewInstance = wv
                }
            )

            // OFFLINE / LOAD ERROR FALLBACK BANNER
            if (loadError != null) {
                Surface(
                    color = NavyDark.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.Center)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = YellowAccent,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Offline Mode / Map Fallback",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Last GPS fix recorded by Android: $latStr, $lngStr",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = YellowAccent
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    loadError = null
                                    webViewInstance?.loadUrl(TRACKING_URL)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                            ) {
                                Text("RETRY WEBSITE")
                            }
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open map: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Text("OPEN GOOGLE MAPS")
                            }
                        }
                    }
                }
            }

            // FLOATING CENTER GPS BUTTON
            FloatingActionButton(
                onClick = {
                    viewModel.requestImmediateGpsFix()
                    webViewInstance?.let { wv ->
                        pushLocationToWebView(wv, actualLat, actualLng)
                        val msg = if (isGpsLocked) "Centering map on real GPS: $latStr, $lngStr" else "Requesting fresh GPS fix from satellites..."
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                containerColor = YellowAccent,
                contentColor = NavyDark,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .testTag("fab_center_gps")
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Center GPS"
                )
            }

            // TOP-LEFT LIVE COORDINATES PILL
            Surface(
                color = NavyDark.copy(alpha = 0.85f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = YellowAccent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$latStr, $lngStr",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // 3. ACTION CONTROLS PANEL (BELOW MAP)
        Surface(
            color = NavyCardDark,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Info Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "REAL-TIME GPS TELEMETRY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = YellowAccent
                        )
                        Text(
                            text = state.lastKnownLocation?.formattedTime ?: "30s Continuous Sync Active",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }

                    Surface(
                        color = SafeGreen.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "±${state.lastKnownLocation?.accuracy?.toInt() ?: tracker.accuracyMeters}m GPS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SafeGreen,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Open Google Maps
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mapsUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open map: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NavyPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_osm_google_maps")
                    ) {
                        Icon(imageVector = Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "GOOGLE MAPS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Silent SMS
                    Button(
                        onClick = { viewModel.dispatchSilentLocationSmsManual() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafeGreen,
                            contentColor = NavyDark
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .testTag("btn_osm_silent_sms")
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "SILENT SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // Copy Coordinates
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Coordinates", "$latStr, $lngStr"))
                            Toast.makeText(context, "Coordinates copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier.size(16.dp),
                            tint = YellowAccent
                        )
                    }
                }

                // Expandable Location History Sheet
                AnimatedVisibility(visible = showHistorySheet) {
                    Column(modifier = Modifier.padding(top = 12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recorded GPS Fixes (${state.locationHistory.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (state.locationHistory.isNotEmpty()) {
                                IconButton(
                                    onClick = { viewModel.clearLocationHistory() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Clear",
                                        tint = AlertRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(state.locationHistory.take(15)) { item ->
                                Surface(
                                    color = NavyDark,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "${String.format(Locale.US, "%.5f", item.latitude)}, ${String.format(Locale.US, "%.5f", item.longitude)}",
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = YellowAccent
                                            )
                                            Text(
                                                text = "${item.formattedTime} • ±${item.accuracy.toInt()}m",
                                                fontSize = 9.sp,
                                                color = Color.LightGray
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                webViewInstance?.let { wv ->
                                                    pushLocationToWebView(wv, item.latitude, item.longitude)
                                                    Toast.makeText(context, "Map centered on fix", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.GpsFixed,
                                                contentDescription = "Show on Map",
                                                tint = SafeGreen,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.destroy()
            webViewInstance = null
        }
    }
}
