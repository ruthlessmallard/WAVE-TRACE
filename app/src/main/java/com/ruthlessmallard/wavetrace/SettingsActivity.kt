package com.ruthlessmallard.wavetrace

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var scanFrequencyEditText: EditText
    private lateinit var dbiCalibrationEditText: EditText
    private lateinit var toneFeedbackSwitch: SwitchCompat
    private lateinit var toneThresholdEditText: EditText
    private lateinit var saveSettingsButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize UI elements
        scanFrequencyEditText = findViewById(R.id.scanFrequencyEditText)
        dbiCalibrationEditText = findViewById(R.id.dbiCalibrationEditText)
        toneFeedbackSwitch = findViewById(R.id.toneFeedbackSwitch)
        toneThresholdEditText = findViewById(R.id.toneThresholdEditText)
        saveSettingsButton = findViewById(R.id.saveSettingsButton)

        loadSettings()

        saveSettingsButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSettings() {
        val sharedPrefs = getSharedPreferences("WaveTraceSettings", Context.MODE_PRIVATE)
        val scanFrequency = sharedPrefs.getInt("scanFrequency", 5) // Default to 5 seconds
        val dbiCalibration = sharedPrefs.getInt("dbiCalibration", 0) // Default to 0
        val toneEnabled = sharedPrefs.getBoolean("toneFeedbackEnabled", true)
        val toneThreshold = sharedPrefs.getInt("toneThresholdDb", 2)

        scanFrequencyEditText.setText(scanFrequency.toString())
        dbiCalibrationEditText.setText(dbiCalibration.toString())
        toneFeedbackSwitch.isChecked = toneEnabled
        toneThresholdEditText.setText(toneThreshold.toString())
    }

    private fun saveSettings() {
        val sharedPrefs = getSharedPreferences("WaveTraceSettings", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            try {
                val scanFrequency = scanFrequencyEditText.text.toString().toInt()
                if (scanFrequency <= 0) {
                    Toast.makeText(this@SettingsActivity, "Scan frequency must be greater than 0", Toast.LENGTH_SHORT).show()
                    return
                }
                putInt("scanFrequency", scanFrequency)
            } catch (e: NumberFormatException) {
                Toast.makeText(this@SettingsActivity, "Invalid scan frequency", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val dbiCalibration = dbiCalibrationEditText.text.toString().toInt()
                putInt("dbiCalibration", dbiCalibration)
            } catch (e: NumberFormatException) {
                Toast.makeText(this@SettingsActivity, "Invalid DBI calibration", Toast.LENGTH_SHORT).show()
                return
            }

            try {
                val toneThreshold = toneThresholdEditText.text.toString().toInt()
                if (toneThreshold <= 0) {
                    Toast.makeText(this@SettingsActivity, "Tone threshold must be greater than 0", Toast.LENGTH_SHORT).show()
                    return
                }
                putInt("toneThresholdDb", toneThreshold)
            } catch (e: NumberFormatException) {
                Toast.makeText(this@SettingsActivity, "Invalid tone threshold", Toast.LENGTH_SHORT).show()
                return
            }

            putBoolean("toneFeedbackEnabled", toneFeedbackSwitch.isChecked)
            apply()
        }
        Toast.makeText(this, "Settings Saved", Toast.LENGTH_SHORT).show()
        finish() // Close settings activity
    }
}
