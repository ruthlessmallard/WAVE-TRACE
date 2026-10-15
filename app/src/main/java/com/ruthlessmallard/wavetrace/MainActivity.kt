package com.ruthlessmallard.wavetrace

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var signalGraph: SignalGraphView
    private lateinit var ssidText: TextView
    private lateinit var rssiText: TextView
    private lateinit var bssidText: TextView
    private lateinit var frequencyText: TextView
    private lateinit var linkSpeedText: TextView
    private lateinit var statusText: TextView

    private lateinit var networkRecyclerView: RecyclerView
    private lateinit var settingsButton: ImageButton
    private lateinit var mainContentLayout: View
    private lateinit var detailsGridLayout: View
    private lateinit var networkAdapter: NetworkAdapter
    private lateinit var scanButton: ImageButton

    private val availableNetworks = mutableListOf<ScanResult>()

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 1000L // 1 second

    // User-configurable via the Settings screen; reloaded on every resume
    private val prefs by lazy { getSharedPreferences("WaveTraceSettings", Context.MODE_PRIVATE) }
    private var scanIntervalMs = 5000L        // scanFrequency (seconds) * 1000
    private var dbiCalibration = 0            // offset added to every displayed reading

    // Tone feedback: a rising/falling chirp when RSSI moves beyond a threshold
    private var toneEnabled = true
    private var toneThresholdDb = 5
    private var lastToneReading: Float? = null
    private var lastToneTimeMs = 0L
    private val toneFeedback by lazy { ToneFeedback() }

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateWifiInfo()
            handler.postDelayed(this, updateInterval)
        }
    }

    private val wifiScanReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION == intent?.action) {
                val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                if (success) {
                    updateNetworkList()
                }
            }
        }
    }

    private var isShowingNetworkList = false
    private var selectedNetwork: ScanResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        // Initialize existing UI elements
        ssidText = findViewById(R.id.ssidText)
        rssiText = findViewById(R.id.rssiText)
        bssidText = findViewById(R.id.bssidText)
        frequencyText = findViewById(R.id.frequencyText)
        linkSpeedText = findViewById(R.id.linkSpeedText)
        statusText = findViewById(R.id.statusText)
        signalGraph = findViewById(R.id.signalGraph)

        // Initialize new UI elements
        networkRecyclerView = findViewById(R.id.networkRecyclerView)
        settingsButton = findViewById(R.id.settingsButton)
        scanButton = findViewById(R.id.scanButton)
        mainContentLayout = findViewById(R.id.mainContentLayout)
        detailsGridLayout = findViewById(R.id.detailsGridLayout)

        // Setup RecyclerView
        networkAdapter = NetworkAdapter(availableNetworks) { network ->
            selectNetwork(network)
        }
        networkRecyclerView.layoutManager = LinearLayoutManager(this)
        networkRecyclerView.adapter = networkAdapter

        scanButton.setOnClickListener {
            toggleNetworkListView(!isShowingNetworkList)
        }
        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        checkPermissions()

        // Register receiver for Wi-Fi scan results
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        registerReceiver(wifiScanReceiver, intentFilter)
    }

    override fun onResume() {
        super.onResume()
        reloadSettings()
        handler.post(updateRunnable)
        // Start continuous scanning when the app is active and list is visible
        if (isShowingNetworkList) {
            startWifiScan()
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
        // Stop continuous scanning when app is paused
        handler.removeCallbacks(scanRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(wifiScanReceiver)
        toneFeedback.release()
    }

    private fun toggleNetworkListView(showList: Boolean) {
        isShowingNetworkList = showList
        if (showList) {
            mainContentLayout.visibility = View.GONE
            detailsGridLayout.visibility = View.GONE
            networkRecyclerView.visibility = View.VISIBLE
            startWifiScan()
            statusText.text = "SELECT NETWORK..."
            statusText.setTextColor(getColor(R.color.accentColor))
        } else {
            networkRecyclerView.visibility = View.GONE
            mainContentLayout.visibility = View.VISIBLE
            detailsGridLayout.visibility = View.VISIBLE
            handler.removeCallbacks(scanRunnable) // Stop scanning when not in list view
            // Resume connected info update (if it was paused or changed)
            updateWifiInfo()
        }
    }

    private fun selectNetwork(network: ScanResult) {
        selectedNetwork = network
        toggleNetworkListView(false) // Switch back to main view
        signalGraph.clearReadings() // Clear graph for new network
        statusText.text = "MONITORING ${selectedNetwork?.SSID}"
        statusText.setTextColor(getColor(R.color.accentColor))
    }

    private fun reloadSettings() {
        scanIntervalMs = prefs.getInt("scanFrequency", 5).coerceAtLeast(1) * 1000L
        dbiCalibration = prefs.getInt("dbiCalibration", 0)
        toneEnabled = prefs.getBoolean("toneFeedbackEnabled", true)
        toneThresholdDb = prefs.getInt("toneThresholdDb", 5).coerceAtLeast(1)
        // Reset the baseline so returning from Settings can't fire a stale cue
        lastToneReading = null
    }

    /**
     * Wi-Fi scanning needs ACCESS_FINE_LOCATION on every version, plus
     * NEARBY_WIFI_DEVICES at runtime on Android 13+. Requesting only
     * NEARBY_WIFI_DEVICES (without the neverForLocation flag) yields EMPTY
     * scan results — the bug that broke the first polish build.
     */
    private fun scanPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        else
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    private fun hasScanPermission(): Boolean =
        scanPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun startWifiScan() {
        if (hasScanPermission()) {
            wifiManager.startScan()
            handler.postDelayed(scanRunnable, scanIntervalMs)
        } else {
            // Permissions not granted, cannot scan
            statusText.text = "SCAN PERMISSION REQUIRED"
            statusText.setTextColor(getColor(R.color.vitalsCritical))
        }
    }

    private val scanRunnable = object : Runnable {
        override fun run() {
            startWifiScan()
        }
    }

    private fun updateNetworkList() {
        if (!hasScanPermission()) {
            // Permissions are not granted, handle this case (should be caught by checkPermissions)
            return
        }
        val scanResults = wifiManager.scanResults
        availableNetworks.clear()
        // Filter out networks with no SSID and remove duplicates by SSID
        availableNetworks.addAll(scanResults.filter { !it.SSID.isNullOrBlank() }.distinctBy { it.SSID })
        networkAdapter.notifyDataSetChanged()
    }

    private fun checkPermissions() {
        val requiredPermissions = scanPermissions() + arrayOf(
            Manifest.permission.ACCESS_WIFI_STATE, // For general Wi-Fi state access
            Manifest.permission.CHANGE_WIFI_STATE // For initiating scans
        )
        val missingPermissions = requiredPermissions.filter { 
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                LOCATION_PERMISSION_REQUEST
            )
        } else {
            statusText.text = "PERMISSIONS GRANTED"
            statusText.setTextColor(getColor(R.color.vitalsStable))
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (hasScanPermission()) {
                statusText.text = "SCANNING..."
                statusText.setTextColor(getColor(R.color.vitalsStable))
                if (isShowingNetworkList) {
                    startWifiScan()
                }
            } else {
                statusText.text = "SCAN PERMISSION NOT GRANTED"
                statusText.setTextColor(getColor(R.color.vitalsCritical))
            }
        }
    }

    private fun updateWifiInfo() {
        try {
            // Only update connected info if not showing the network list
            if (!isShowingNetworkList) {
                val rawRssi: Int?
                val currentSsid: String
                val currentBssid: String
                val currentFrequency: Int
                val currentLinkSpeed: Int

                if (selectedNetwork != null) {
                    // Monitor the selected network
                    val scanResult = availableNetworks.find { it.BSSID == selectedNetwork?.BSSID }
                    if (scanResult != null) {
                        rawRssi = scanResult.level
                        currentSsid = scanResult.SSID
                        currentBssid = scanResult.BSSID
                        currentFrequency = scanResult.frequency
                        currentLinkSpeed = -1 // ScanResult doesn't have link speed
                    } else {
                        // Selected network dropped out of the scan window
                        rawRssi = null
                        currentSsid = selectedNetwork?.SSID ?: "Lost Network"
                        currentBssid = selectedNetwork?.BSSID ?: "Unknown"
                        currentFrequency = 0
                        currentLinkSpeed = -1
                        // Re-initiate scan to find it again
                        startWifiScan()
                    }
                } else {
                    // Fall back to the currently-connected AP
                    val connectionInfo = wifiManager.connectionInfo
                    val ssid = connectionInfo.ssid?.replace("\"", "")
                    if (ssid.isNullOrBlank() || ssid == "<unknown ssid>" || connectionInfo.networkId == -1) {
                        rawRssi = null
                        currentSsid = "NO NETWORK SELECTED"
                        currentBssid = "Unknown"
                        currentFrequency = 0
                        currentLinkSpeed = -1
                    } else {
                        rawRssi = connectionInfo.rssi
                        currentSsid = ssid
                        currentBssid = connectionInfo.bssid ?: "Unknown"
                        currentFrequency = connectionInfo.frequency
                        currentLinkSpeed = connectionInfo.linkSpeed
                    }
                }

                ssidText.text = currentSsid
                bssidText.text = if (currentBssid.isBlank() || currentBssid == "Unknown") "--" else currentBssid.uppercase()
                frequencyText.text = if (currentFrequency > 0) "${currentFrequency} MHz" else "-- MHz"
                linkSpeedText.text = if (currentLinkSpeed > 0) "${currentLinkSpeed} Mbps" else "-- Mbps"

                if (rawRssi == null) {
                    // No reading to report — don't invent one
                    lastToneReading = null
                    rssiText.text = "-- dBm"
                    rssiText.setTextColor(getColor(R.color.vitalsFlatline))
                    statusText.text = if (selectedNetwork != null) "LOST ${currentSsid}"
                                       else "TAP THE SCAN ICON TO MONITOR A NETWORK"
                    statusText.setTextColor(getColor(R.color.vitalsCritical))
                    return
                }

                // Apply the user's calibration offset, then display and plot it
                val calibratedRssi = rawRssi + dbiCalibration
                rssiText.text = "${calibratedRssi} dBm"
                signalGraph.addReading(calibratedRssi.toFloat())
                maybeTone(calibratedRssi.toFloat())

                // Status color based on signal strength
                when {
                    calibratedRssi >= -50 -> {
                        statusText.text = "SIGNAL EXCELLENT"
                        statusText.setTextColor(getColor(R.color.vitalsStable))
                        rssiText.setTextColor(getColor(R.color.vitalsStable))
                    }
                    calibratedRssi >= -65 -> {
                        statusText.text = "SIGNAL GOOD"
                        statusText.setTextColor(getColor(R.color.vitalsStable))
                        rssiText.setTextColor(getColor(R.color.vitalsStable))
                    }
                    calibratedRssi >= -75 -> {
                        statusText.text = "SIGNAL FAIR"
                        statusText.setTextColor(getColor(R.color.vitalsCaution))
                        rssiText.setTextColor(getColor(R.color.vitalsCaution))
                    }
                    calibratedRssi >= -85 -> {
                        statusText.text = "SIGNAL POOR"
                        statusText.setTextColor(getColor(R.color.vitalsCritical))
                        rssiText.setTextColor(getColor(R.color.vitalsCritical))
                    }
                    else -> {
                        statusText.text = "SIGNAL CRITICAL"
                        statusText.setTextColor(getColor(R.color.vitalsCritical))
                        rssiText.setTextColor(getColor(R.color.vitalsCritical))
                    }
                }
            }
        } catch (e: SecurityException) {
            statusText.text = "PERMISSION ERROR"
            statusText.setTextColor(getColor(R.color.vitalsCritical))
        }
    }

    /**
     * Play a rising cue when the reading climbs past the threshold and a
     * falling cue when it drops past it. The threshold is a dB delta, not a
     * percentage: RSSI is logarithmic, so a percent of a negative dBm value is
     * meaningless. A minimum gap between cues stops a drifting signal from
     * machine-gunning the speaker.
     */
    private fun maybeTone(reading: Float) {
        val previous = lastToneReading
        lastToneReading = reading
        if (!toneEnabled || previous == null) return

        val delta = reading - previous
        if (Math.abs(delta) < toneThresholdDb) return

        val now = System.currentTimeMillis()
        if (now - lastToneTimeMs < MIN_TONE_GAP_MS) return
        lastToneTimeMs = now

        if (delta > 0) toneFeedback.rising() else toneFeedback.falling()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
        private const val MIN_TONE_GAP_MS = 1200L
    }
}
