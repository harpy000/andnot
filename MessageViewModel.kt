package com.whatsappvault.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.whatsappvault.database.AppDatabase
import com.whatsappvault.database.MessageEntity

class MessageViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getDatabase(application).messageDao()

    // Filter state
    private val _filter = MutableLiveData<Filter>(Filter.ALL)
    val filter: LiveData<Filter> = _filter

    // Search query
    private val _searchQuery = MutableLiveData<String>("")

    // Messages based on current filter/search
    val messages: LiveData<List<MessageEntity>> = _filter.switchMap { f ->
        when (f) {
            Filter.ALL -> dao.getAllMessages()
            Filter.DELETED -> dao.getDeletedMessages()
        }
    }

    fun setFilter(f: Filter) {
        _filter.value = f
    }

    fun searchMessages(query: String): LiveData<List<MessageEntity>> {
        return dao.searchMessages(query)
    }

    enum class Filter { ALL, DELETED }
}
