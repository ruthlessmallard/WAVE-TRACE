package com.ruthlessmallard.wavetrace

import android.net.wifi.ScanResult
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NetworkViewHolder(itemView: View, private val onItemClick: (ScanResult) -> Unit) : RecyclerView.ViewHolder(itemView) {
    private val networkNameTextView: TextView = itemView.findViewById(R.id.networkNameTextView)
    private val networkRssiTextView: TextView = itemView.findViewById(R.id.networkRssiTextView)
    private var currentNetwork: ScanResult? = null

    init {
        itemView.setOnClickListener { 
            currentNetwork?.let(onItemClick) 
        }
    }

    fun bind(network: ScanResult) {
        currentNetwork = network
        networkNameTextView.text = network.SSID
        networkRssiTextView.text = "${network.level} dBm"

        // You can add more detailed styling based on signal strength here if desired
        // For example:
        // when {
        //    network.level >= -50 -> networkRssiTextView.setTextColor(itemView.context.getColor(R.color.vitalsStable))
        //    network.level >= -65 -> networkRssiTextView.setTextColor(itemView.context.getColor(R.color.vitalsStable))
        //    else -> networkRssiTextView.setTextColor(itemView.context.getColor(R.color.vitalsCritical))
        // }
    }
}
