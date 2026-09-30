package com.ruthlessmallard.wavetrace

import android.Manifest
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var wifiManager: WifiManager
    private lateinit var signalGraph: SignalGraphView
    private lateinit var ssidText: TextView
    private lateinit var rssiText: TextView
    private lateinit var bssidText: TextView
    private lateinit var frequencyText: TextView
    private lateinit var linkSpeedText: TextView
    private lateinit var statusText: TextView

    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 1000L // 1 second

    private val updateRunnable = object : Runnable {
        override fun run() {
            updateWifiInfo()
            handler.postDelayed(this, updateInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager

        ssidText = findViewById(R.id.ssidText)
        rssiText = findViewById(R.id.rssiText)
        bssidText = findViewById(R.id.bssidText)
        frequencyText = findViewById(R.id.frequencyText)
        linkSpeedText = findViewById(R.id.linkSpeedText)
        statusText = findViewById(R.id.statusText)
        signalGraph = findViewById(R.id.signalGraph)

        checkPermissions()
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateRunnable)
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                statusText.text = "SCANNING..."
                statusText.setTextColor(getColor(R.color.vitalsStable))
            } else {
                statusText.text = "LOCATION PERMISSION REQUIRED"
                statusText.setTextColor(getColor(R.color.vitalsCritical))
            }
        }
    }

    private fun updateWifiInfo() {
        try {
            val connectionInfo = wifiManager.connectionInfo
            val rssi = connectionInfo.rssi
            val ssid = connectionInfo.ssid?.replace("\"", "") ?: "Unknown"
            val bssid = connectionInfo.bssid ?: "Unknown"
            val frequency = connectionInfo.frequency
            val linkSpeed = connectionInfo.linkSpeed

            ssidText.text = ssid
            rssiText.text = "${rssi} dBm"
            bssidText.text = bssid.uppercase()
            frequencyText.text = "${frequency} MHz"
            linkSpeedText.text = "${linkSpeed} Mbps"

            // Add to graph
            signalGraph.addReading(rssi.toFloat())

            // Status color based on signal strength
            when {
                rssi >= -50 -> {
                    statusText.text = "SIGNAL EXCELLENT"
                    statusText.setTextColor(getColor(R.color.vitalsStable))
                    rssiText.setTextColor(getColor(R.color.vitalsStable))
                }
                rssi >= -65 -> {
                    statusText.text = "SIGNAL GOOD"
                    statusText.setTextColor(getColor(R.color.vitalsStable))
                    rssiText.setTextColor(getColor(R.color.vitalsStable))
                }
                rssi >= -75 -> {
                    statusText.text = "SIGNAL FAIR"
                    statusText.setTextColor(getColor(R.color.vitalsCaution))
                    rssiText.setTextColor(getColor(R.color.vitalsCaution))
                }
                rssi >= -85 -> {
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

        } catch (e: SecurityException) {
            statusText.text = "PERMISSION ERROR"
            statusText.setTextColor(getColor(R.color.vitalsCritical))
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST = 1001
    }
}
