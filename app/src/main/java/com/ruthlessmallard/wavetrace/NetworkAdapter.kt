package com.ruthlessmallard.wavetrace

import android.net.wifi.ScanResult
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class NetworkAdapter(
    private val networks: MutableList<ScanResult>,
    private val onItemClick: (ScanResult) -> Unit
) : RecyclerView.Adapter<NetworkViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NetworkViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.network_item, parent, false)
        return NetworkViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: NetworkViewHolder, position: Int) {
        val network = networks[position]
        holder.bind(network)
    }

    override fun getItemCount(): Int = networks.size
}
