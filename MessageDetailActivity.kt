package com.whatsappvault.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.whatsappvault.database.AppDatabase
import com.whatsappvault.database.MessageEntity
import com.whatsappvault.databinding.ActivityMessageDetailBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MessageDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMessageDetailBinding
    private val sdf = SimpleDateFormat("EEEE, MMM d yyyy\nHH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessageDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val messageId = intent.getLongExtra("message_id", -1)
        if (messageId == -1L) { finish(); return }

        loadMessage(messageId)
    }

    private fun loadMessage(id: Long) {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val msg = withContext(Dispatchers.IO) {
                db.messageDao().getMessageById(id)
            } ?: run { finish(); return@launch }

            displayMessage(msg)
        }
    }

    private fun displayMessage(msg: MessageEntity) {
        supportActionBar?.title = msg.sender

        binding.tvSender.text = msg.sender
        binding.tvMessage.text = msg.message.ifBlank { "(No text — image message)" }
        binding.tvCapturedAt.text = "Captured: ${sdf.format(Date(msg.timestamp))}"

        if (msg.isDeleted) {
            binding.deletedBanner.visibility = View.VISIBLE
            binding.tvDeletedAt.text = msg.deletedAt?.let {
                "Deleted: ${sdf.format(Date(it))}"
            } ?: "Marked as deleted"
        } else {
            binding.deletedBanner.visibility = View.GONE
        }

        if (msg.isGroup) {
            binding.tvGroup.visibility = View.VISIBLE
            binding.tvGroup.text = "Group: ${msg.groupName}"
        } else {
            binding.tvGroup.visibility = View.GONE
        }

        // Show image if available
        if (msg.imageCopyPath != null) {
            val file = File(msg.imageCopyPath)
            if (file.exists()) {
                binding.imgMessage.visibility = View.VISIBLE
                binding.tvImageSaved.visibility = View.VISIBLE
                Glide.with(this)
                    .load(file)
                    .into(binding.imgMessage)
            } else {
                binding.tvImageSaved.visibility = View.VISIBLE
                binding.tvImageSaved.text = "⚠️ Image file not found"
            }
        } else {
            binding.imgMessage.visibility = View.GONE
            binding.tvImageSaved.visibility = View.GONE
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
