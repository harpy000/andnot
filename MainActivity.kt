package com.whatsappvault.ui

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.whatsappvault.R
import com.whatsappvault.databinding.ActivityMainBinding
import com.whatsappvault.service.ImageObserverService
import com.whatsappvault.service.WhatsAppNotificationListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MessageViewModel
    private lateinit var adapter: MessageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        viewModel = ViewModelProvider(this)[MessageViewModel::class.java]

        setupRecyclerView()
        setupFilterChips()
        checkPermissions()
        startImageObserver()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun setupRecyclerView() {
        adapter = MessageAdapter { message ->
            val intent = Intent(this, MessageDetailActivity::class.java)
            intent.putExtra("message_id", message.id)
            startActivity(intent)
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
        }

        viewModel.messages.observe(this) { messages ->
            adapter.submitList(messages)
            binding.emptyView.visibility =
                if (messages.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            binding.messageCount.text = "${messages.size} messages"
        }
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener {
            viewModel.setFilter(MessageViewModel.Filter.ALL)
        }
        binding.chipDeleted.setOnClickListener {
            viewModel.setFilter(MessageViewModel.Filter.DELETED)
        }
    }

    private fun checkPermissions() {
        if (!isNotificationListenerEnabled()) {
            showPermissionDialog()
        }
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false

        val cn = ComponentName(this, WhatsAppNotificationListener::class.java)
        return flat.contains(cn.flattenToString())
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage(
                "WhatsApp Vault needs Notification Access permission to capture messages.\n\n" +
                "Tap OK to go to Settings, then find 'WhatsApp Vault' and enable it."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
            .setNegativeButton("Later", null)
            .setCancelable(false)
            .show()
    }

    private fun startImageObserver() {
        val intent = Intent(this, ImageObserverService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun updateServiceStatus() {
        val enabled = isNotificationListenerEnabled()
        binding.statusIndicator.apply {
            text = if (enabled) "● Active" else "● Inactive - tap to enable"
            setTextColor(
                if (enabled)
                    getColor(android.R.color.holo_green_dark)
                else
                    getColor(android.R.color.holo_red_dark)
            )
            if (!enabled) {
                setOnClickListener { showPermissionDialog() }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                if (!newText.isNullOrBlank()) {
                    viewModel.searchMessages(newText).observe(this@MainActivity) { results ->
                        adapter.submitList(results)
                    }
                } else {
                    viewModel.messages.value?.let { adapter.submitList(it) }
                }
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
