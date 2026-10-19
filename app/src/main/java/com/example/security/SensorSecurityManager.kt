package com.example.security

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SensorTelemetry(
    val proximityCm: Float = 5f,
    val isProximityNear: Boolean = false,
    val lightLux: Float = 250f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 9.8f,
    val totalAcceleration: Float = 9.8f,
    val isPowerConnected: Boolean = false,
    val isUsbConnected: Boolean = false,
    val batteryPct: Int = 100,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val accuracy: Float = 5.0f,
    val speed: Float = 0.0f
) {
    val isInPocket: Boolean get() = isProximityNear && lightLux < 25f
    val isWalkingFiltered: Boolean get() = false
    val liftDetected: Boolean get() = totalAcceleration > 14.0f
}

data class GuardConfig(
    val chargerUnplugEnabled: Boolean = true,
    val usbConnectionEnabled: Boolean = true,
    val simProtectionEnabled: Boolean = true,
    val wrongPinAlarmEnabled: Boolean = true,
    val liveGpsTrackingEnabled: Boolean = true,
    val intruderSelfieEnabled: Boolean = true,
    val pocketDetectionEnabled: Boolean = false,
    val motionDetectionEnabled: Boolean = false,
    val motionSensitivity: Float = 2.0f,
    val armingDelaySeconds: Int = 30
)

enum class TriggerReason(val title: String, val patternKey: String) {
    CHARGER_UNPLUGGED("Power Disconnected", "CHARGER_UNPLUG"),
    USB_CONNECTED("USB Connected", "USB_CONNECTION"),
    SIM_CHANGED("SIM Card Changed / Removed", "SIM_CHANGE"),
    WRONG_PIN_ATTEMPTS("Wrong PIN Entered 3 Times", "WRONG_PIN"),
    INTRUDER_FAILED_PIN("Failed Master PIN Attempt", "WRONG_PIN"),
    MANUAL_PANIC("Manual Emergency Siren", "EMERGENCY_ALARM"),
    POCKET_REMOVAL("Pocket Extraction Theft Detected", "POCKET_REMOVAL"),
    HAND_GRAB_DETECTED("Hand Touch / Grab Detected", "POCKET_REMOVAL"),
    MOTION_DETECTED("Unauthorized Motion Detected", "MOTION_DETECTION"),
    SLEEP_TOUCH_BREACH("Sleep Guard: Unauthorized Touch", "EMERGENCY_ALARM")
}

/**
 * SensorSecurityManager - Simple Manual Security Mode:
 * - Big ARM ALARM with 30-second countdown.
 * - ZERO false alarms from pockets or hand touches.
 * - Strictly triggers only on:
 *     1. Power disconnect
 *     2. USB connect
 *     3. SIM change
 *     4. Wrong PIN 3 times
 * - Disarm with Master PIN or Voice "Disarm [PIN]".
 */
class SensorSecurityManager(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val proximitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    private val lightSensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    val batteryOptimizer = BatteryOptimizer.getInstance(context)

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _telemetry = MutableStateFlow(SensorTelemetry())
    val telemetry: StateFlow<SensorTelemetry> = _telemetry.asStateFlow()

    private val _isArmed = MutableStateFlow(false)
    val isArmed: StateFlow<Boolean> = _isArmed.asStateFlow()

    private val _isArmingCountdown = MutableStateFlow(false)
    val isArmingCountdown: StateFlow<Boolean> = _isArmingCountdown.asStateFlow()

    private val _countdownRemaining = MutableStateFlow(0)
    val countdownRemaining: StateFlow<Int> = _countdownRemaining.asStateFlow()

    private val _activeTrigger = MutableStateFlow<TriggerReason?>(null)
    val activeTrigger: StateFlow<TriggerReason?> = _activeTrigger.asStateFlow()

    private val _failedPinAttempts = MutableStateFlow(0)
    val failedPinAttempts: StateFlow<Int> = _failedPinAttempts.asStateFlow()

    private var wasPluggedInWhenArmed: Boolean = false
    private var wasUsbConnectedWhenArmed: Boolean = false
    private var areSensorsRegistered: Boolean = false
    private var currentGuardConfig: GuardConfig = GuardConfig()

    private var countdownJob: Job? = null
    private var locationCallback: LocationCallback? = null

    var onTriggerAlarm: ((TriggerReason) -> Unit)? = null
    var onPhoneLifted: (() -> Unit)? = null

    private val powerAndSimReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    _telemetry.update { it.copy(isPowerConnected = true) }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    _telemetry.update { it.copy(isPowerConnected = false) }
                    // 1. Power disconnect trigger
                    if (_isArmed.value && currentGuardConfig.chargerUnplugEnabled) {
                        fireTrigger(TriggerReason.CHARGER_UNPLUGGED)
                    }
                }
                "android.hardware.usb.action.USB_STATE" -> {
                    val connected = intent.extras?.getBoolean("connected") ?: false
                    _telemetry.update { it.copy(isUsbConnected = connected) }
                    // 2. USB connect trigger
                    if (_isArmed.value && currentGuardConfig.usbConnectionEnabled && connected && !wasUsbConnectedWhenArmed) {
                        fireTrigger(TriggerReason.USB_CONNECTED)
                    }
                }
                "android.intent.action.SIM_STATE_CHANGED" -> {
                    val ss = intent.getStringExtra("ss")
                    // 3. SIM change trigger
                    if (_isArmed.value && currentGuardConfig.simProtectionEnabled) {
                        if (ss == "ABSENT" || ss == "LOCKED" || ss == "CARD_IO_ERROR" || ss == "UNKNOWN") {
                            fireTrigger(TriggerReason.SIM_CHANGED)
                        }
                    }
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    val pct = if (level != -1 && scale > 0) (level * 100) / scale else 100
                    val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                    val isPlugged = plugged != 0
                    val isUsb = plugged == BatteryManager.BATTERY_PLUGGED_USB
                    _telemetry.update {
                        it.copy(
                            batteryPct = pct,
                            isPowerConnected = isPlugged,
                            isUsbConnected = isUsb
                        )
                    }
                }
            }
        }
    }

    init {
        registerSensors()
        registerReceivers()
        fetchInitialRealLocation()
    }

    fun fetchInitialRealLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    _telemetry.update {
                        it.copy(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy
                        )
                    }
                }
            }
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            lm?.let {
                val loc = it.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: it.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: it.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
                if (loc != null) {
                    _telemetry.update { t ->
                        t.copy(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            accuracy = loc.accuracy
                        )
                    }
                }
            }
        } catch (_: SecurityException) {}
    }

    /**
     * Called when a wrong PIN is entered.
     * After 3 consecutive wrong attempts, triggers WRONG_PIN_ATTEMPTS alarm!
     */
    fun recordFailedPinAttempt(): Int {
        val count = _failedPinAttempts.value + 1
        _failedPinAttempts.value = count
        if (count >= 3 && currentGuardConfig.wrongPinAlarmEnabled) {
            fireTrigger(TriggerReason.WRONG_PIN_ATTEMPTS)
        }
        return count
    }

    fun resetFailedPinAttempts() {
        _failedPinAttempts.value = 0
    }

    fun registerSensors() {
        if (areSensorsRegistered) return
        val delay = batteryOptimizer.getSensorDelay()
        sensorManager?.let { sm ->
            proximitySensor?.let { sm.registerListener(this, it, delay) }
            lightSensor?.let { sm.registerListener(this, it, delay) }
            accelerometer?.let { sm.registerListener(this, it, delay) }
        }
        areSensorsRegistered = true
    }

    fun unregisterSensors() {
        if (!areSensorsRegistered) return
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}
        areSensorsRegistered = false
    }

    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction("android.hardware.usb.action.USB_STATE")
            addAction("android.intent.action.SIM_STATE_CHANGED")
        }
        try {
            context.registerReceiver(powerAndSimReceiver, filter)
        } catch (_: Exception) {}
    }

    fun startArmingSequence(delaySec: Int = 30, config: GuardConfig = GuardConfig()) {
        if (_isArmed.value || _isArmingCountdown.value) return

        currentGuardConfig = config
        _isArmingCountdown.value = true
        _countdownRemaining.value = delaySec
        resetFailedPinAttempts()

        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (i in delaySec downTo 1) {
                _countdownRemaining.value = i
                delay(1000)
            }
            _isArmingCountdown.value = false
            _isArmed.value = true
            _countdownRemaining.value = 0

            wasPluggedInWhenArmed = _telemetry.value.isPowerConnected
            wasUsbConnectedWhenArmed = _telemetry.value.isUsbConnected

            if (config.liveGpsTrackingEnabled) {
                startGpsTracking()
            }
        }
    }

    fun disarmSystem() {
        countdownJob?.cancel()
        _isArmingCountdown.value = false
        _isArmed.value = false
        _activeTrigger.value = null
        resetFailedPinAttempts()
        stopGpsTracking()
    }

    fun triggerPanic() {
        fireTrigger(TriggerReason.MANUAL_PANIC)
    }

    fun fireTrigger(reason: TriggerReason) {
        _activeTrigger.value = reason
        onTriggerAlarm?.invoke(reason)
    }

    fun clearTrigger() {
        _activeTrigger.value = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_PROXIMITY -> {
                val dist = event.values[0]
                val maxRange = event.sensor.maximumRange
                val isNear = dist < 3.0f || (dist < maxRange && dist < 4.0f)
                _telemetry.update {
                    it.copy(proximityCm = dist, isProximityNear = isNear)
                }
            }
            Sensor.TYPE_LIGHT -> {
                val lux = event.values[0]
                _telemetry.update {
                    it.copy(lightLux = lux)
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val total = kotlin.math.sqrt((x * x + y * y + z * z).toDouble()).toFloat()
                _telemetry.update {
                    it.copy(
                        accelX = x,
                        accelY = y,
                        accelZ = z,
                        totalAcceleration = total
                    )
                }
                // Note: Hand touches, pockets, and steps are explicitly suppressed in Simple Manual Mode to eliminate all false alarms.
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun startGpsTracking() {
        try {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15_000L)
                .setMinUpdateIntervalMillis(5000L)
                .build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.lastLocation?.let { loc ->
                        _telemetry.update {
                            it.copy(
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                accuracy = loc.accuracy,
                                speed = loc.speed * 3.6f
                            )
                        }
                    }
                }
            }

            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) {}
    }

    fun stopGpsTracking() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
            locationCallback = null
        }
    }

    fun onDestroy() {
        stopGpsTracking()
        countdownJob?.cancel()
        try {
            sensorManager?.unregisterListener(this)
            context.unregisterReceiver(powerAndSimReceiver)
        } catch (_: Exception) {}
    }
}
