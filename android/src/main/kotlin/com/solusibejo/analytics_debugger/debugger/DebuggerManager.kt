package com.solusibejo.analytics_debugger.debugger

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.recyclerview.widget.SortedList
import com.solusibejo.analytics_debugger.event_list.NewEventListener
import com.solusibejo.analytics_debugger.model.DebuggerEventItem
import com.solusibejo.analytics_debugger.model.EventProperty
import com.solusibejo.analytics_debugger.view.BarViewContainer
import com.solusibejo.analytics_debugger.view.BubbleViewContainer
import com.solusibejo.analytics_debugger.view.DebuggerViewContainer
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.*
import javax.net.ssl.HttpsURLConnection
import androidx.core.net.toUri

class DebuggerManager {
    @JvmOverloads
    fun showDebugger(rootActivity: Activity, mode: DebuggerMode, systemOverlay: Boolean = false) {
        Handler(Looper.getMainLooper()).post {
            if (!systemOverlay || checkDrawOverlayPermission(rootActivity)) {
                hideDebuggerThreadUnsafe(rootActivity)
                val windowManager = rootActivity.windowManager
                val displayMetrics = DisplayMetrics()
                val display = windowManager.defaultDisplay
                if (display != null) {
                    windowManager.defaultDisplay.getMetrics(displayMetrics)
                }
                val layoutParams =
                    prepareWindowManagerLayoutParams(rootActivity, displayMetrics, systemOverlay)
                val debuggerViewContainer = createDebuggerView(
                    rootActivity, mode,
                    layoutParams
                )
                windowManager.addView(debuggerViewContainer.view, layoutParams)
                debuggerViewContainer.view.setOnTouchListener(
                    DebuggerTouchHandler(
                        windowManager,
                        layoutParams, debuggerViewContainer
                    )
                )
                debuggerViewContainerRef = WeakReference(debuggerViewContainer)
                for (i in events.size() - 1 downTo 0) {
                    val event = events[i]
                    debuggerViewContainer.showEvent(event)
                }
            }
        }
    }

    private fun writeTrackingCallHeader(connection: HttpsURLConnection) {
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json")
    }

    @Throws(IOException::class)
    private fun writeTrackingCallBody(body: JSONObject, connection: HttpsURLConnection) {
        val bodyBytes = body.toString().toByteArray(charset("UTF-8"))
        val os = connection.outputStream
        os.write(bodyBytes)
        os.close()
    }

    @Throws(JSONException::class)
    private fun buildRequestBody(
        deviceId: String,
        eventName: String,
        eventProps: Map<*, *>
    ): JSONObject {
        val eventProperties = JSONObject(eventProps)
        val body = JSONObject()
        body.put("deviceId", deviceId)
        body.put("eventName", eventName)
        body.put("eventProperties", eventProperties)
        return body
    }

    private fun checkDrawOverlayPermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT >= 23) {
            if (!Settings.canDrawOverlays(activity)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    ("package:" + activity.packageName).toUri()
                )
                activity.startActivityForResult(intent, 101)
                Toast.makeText(
                    activity, "Enable \"Draw over other apps\" in " +
                            "Settings - Apps - Your app to use the debugger and restart the app",
                    Toast.LENGTH_LONG
                ).show()
                false
            } else {
                true
            }
        } else {
            true
        }
    }

    private fun createDebuggerView(
        rootActivity: Activity, mode: DebuggerMode,
        layoutParams: WindowManager.LayoutParams
    ): DebuggerViewContainer {
        val debuggerViewContainer: DebuggerViewContainer
        debuggerViewContainer = if (mode === DebuggerMode.bar) {
            createBarView(rootActivity, layoutParams)
        } else {
            createBubbleView(rootActivity, layoutParams)
        }
        return debuggerViewContainer
    }

    fun publishEvent(timestamp: Long, name: String?, properties: List<EventProperty>) {
        val eventProps: MutableList<Map<String, String>> = ArrayList()
        for (eventProperty in properties) {
            val propMap: MutableMap<String, String> = HashMap()
            propMap["id"] = eventProperty.id
            propMap["name"] = eventProperty.name
            propMap["value"] = eventProperty.value
            eventProps.add(propMap)
        }
        val event = DebuggerEventItem(
            UUID.randomUUID().toString(),
            timestamp, name, eventProps, null
        )
        publishEvent(event)
    }

    private fun publishEvent(event: DebuggerEventItem) {
        Handler(Looper.getMainLooper()).post {
            val debuggerViewContainer = debuggerViewContainerRef.get()
            debuggerViewContainer?.showEvent(event)
            events.add(event)
            eventUpdateListener?.onNewEvent(event)
        }
    }

    fun publishEvent(
        id: String?, timestamp: Long?, name: String?,
        eventProps: List<Map<String, String>>?,
        userProps: List<Map<String, String>>?
    ) {
        val event = DebuggerEventItem(
            id, timestamp, name,
            eventProps, userProps
        )
        publishEvent(event)
    }

    val isEnabled: Boolean
        get() {
            val debuggerViewContainer = debuggerViewContainerRef.get()
            return debuggerViewContainer != null
        }

    private fun prepareWindowManagerLayoutParams(
        context: Context,
        displayMetrics: DisplayMetrics,
        overlay: Boolean
    ): WindowManager.LayoutParams {
        var barHeight = 0
        val resources = context.resources
        val resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        if (resourceId > 0) {
            barHeight = resources.getDimensionPixelSize(resourceId)
        }
        val LAYOUT_FLAG: Int = if (overlay) {
            if (Build.VERSION.SDK_INT >= 26) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }
        } else {
            WindowManager.LayoutParams.TYPE_APPLICATION
        }
        val layoutParams = WindowManager.LayoutParams()
        layoutParams.type = LAYOUT_FLAG
        layoutParams.format = PixelFormat.TRANSLUCENT
        layoutParams.flags =
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        layoutParams.y = (displayMetrics.heightPixels - barHeight) / 2
        layoutParams.x = displayMetrics.widthPixels / 2
        return layoutParams
    }

    private fun createBubbleView(
        rootActivity: Activity,
        layoutParams: WindowManager.LayoutParams
    ): DebuggerViewContainer {
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
        return BubbleViewContainer(rootActivity.layoutInflater)
    }

    private fun createBarView(
        rootActivity: Activity,
        layoutParams: WindowManager.LayoutParams
    ): DebuggerViewContainer {
        layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
        return BarViewContainer(rootActivity.layoutInflater)
    }

    fun hideDebugger(anyActivity: Activity) {
        Handler(Looper.getMainLooper()).post { hideDebuggerThreadUnsafe(anyActivity) }
    }

    private fun hideDebuggerThreadUnsafe(activity: Activity) {
        val debuggerViewContainer = debuggerViewContainerRef.get()
        if (debuggerViewContainer != null) {
            try {
                activity.windowManager.removeView(debuggerViewContainer.view)
                debuggerViewContainerRef = WeakReference(null)
            } catch (ignored: Throwable) {
            }
        }
    }

    private class EventsSorting : SortedList.Callback<DebuggerEventItem>() {
        override fun compare(o1: DebuggerEventItem, o2: DebuggerEventItem): Int {
            return o2.timestamp?.compareTo(o1.timestamp ?: 0) ?: 0
        }

        override fun onChanged(position: Int, count: Int) {}
        override fun areContentsTheSame(
            oldItem: DebuggerEventItem,
            newItem: DebuggerEventItem
        ): Boolean {
            return oldItem.equals(newItem)
        }

        override fun areItemsTheSame(item1: DebuggerEventItem, item2: DebuggerEventItem): Boolean {
            return item1 === item2
        }

        override fun onInserted(position: Int, count: Int) {}
        override fun onRemoved(position: Int, count: Int) {}
        override fun onMoved(fromPosition: Int, toPosition: Int) {}
    }

    companion object {
        var events = SortedList(
            DebuggerEventItem::class.java,
            EventsSorting()
        )
        var eventUpdateListener: NewEventListener? = null
        var debuggerViewContainerRef = WeakReference<DebuggerViewContainer?>(null)
    }
}