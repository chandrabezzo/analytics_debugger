package com.solusibejo.analytics_debugger.event_list

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.solusibejo.analytics_debugger.debugger.DebuggerManager
import com.solusibejo.analytics_debugger.R
import com.solusibejo.analytics_debugger.model.DebuggerEventItem

class DebuggerEventsListActivity : AppCompatActivity() {
    var adapter: DebuggerEventsListAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideActionBar()
        setContentView(R.layout.activity_debugger_events_list_activity)
        val recycler = findViewById<RecyclerView>(R.id.events_list)
        adapter = DebuggerEventsListAdapter()
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(this)
        val closeButton = findViewById<View>(R.id.close_button)
        closeButton.setOnClickListener { finish() }
        DebuggerManager.eventUpdateListener =
            object : NewEventListener {
                override fun onNewEvent(event: DebuggerEventItem?) {
                    event.let { it?.let { it1 -> adapter?.onNewItem(it1) } }
                }
            }
    }

    private fun hideActionBar() {
        val actionBar = supportActionBar
        actionBar?.hide()
    }

    override fun onDestroy() {
        super.onDestroy()
        DebuggerManager.eventUpdateListener = null
    }
}