package com.solusibejo.analytics_debugger

import android.app.Activity
import com.solusibejo.analytics_debugger.consts.Arguments
import com.solusibejo.analytics_debugger.debugger.DebuggerManager
import com.solusibejo.analytics_debugger.debugger.DebuggerMode
import com.solusibejo.analytics_debugger.model.EventProperty
import io.flutter.plugin.common.MethodCall

object AnalyticsDebuggerMethods {
    fun show(
        activity: Activity,
        debugger: DebuggerManager,
        call: MethodCall,
    ){
        val mode = call.argument<Boolean>(Arguments.debuggerMode) ?: false
        val isSystemWide = call.argument<Boolean?>(Arguments.systemWide) ?: false
        var type = DebuggerMode.bubble

        if(!mode) type = DebuggerMode.bar

        if(isSystemWide){
            debugger.showDebugger(activity, type, true)
        }
        else {
            debugger.showDebugger(activity, type)
        }
    }

    fun hide(
        activity: Activity,
        debugger: DebuggerManager,
    ) {
        debugger.hideDebugger(activity)
    }

    fun send(
        debugger: DebuggerManager,
        call: MethodCall,
    ){
        val name = call.argument<String>(Arguments.name)
        val values = call.argument<Map<String, Any>?>(Arguments.values)

        val events: ArrayList<EventProperty> = ArrayList()
        if (values != null) {
            for ((key, value) in values){
                events.add(EventProperty(key, value.toString()))
            }
        }

        debugger.publishEvent(
            System.currentTimeMillis(),
            name,
            events
        )
    }
}