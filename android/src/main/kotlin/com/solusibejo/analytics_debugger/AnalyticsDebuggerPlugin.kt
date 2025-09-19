package com.solusibejo.analytics_debugger

import android.app.Activity
import com.solusibejo.analytics_debugger.consts.MethodNames
import com.solusibejo.analytics_debugger.debugger.DebuggerManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

/**
 * AnalyticsDebuggerPlugin
 * 
 * This plugin is built exclusively for Flutter v2 Embedding API.
 * It implements FlutterPlugin and ActivityAware interfaces to provide
 * proper lifecycle management and activity-aware functionality.
 * 
 * Note: This plugin does not support the legacy v1 Flutter Embedding.
 */
class AnalyticsDebuggerPlugin: FlutterPlugin, MethodCallHandler, ActivityAware {
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private lateinit var channel : MethodChannel
  private var activity: Activity? = null
  private var debugger: DebuggerManager? = null

  override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
    channel = MethodChannel(flutterPluginBinding.binaryMessenger, "analytics_debugger")
    channel.setMethodCallHandler(this)
  }

  override fun onMethodCall(call: MethodCall, result: Result) {
    if(activity != null && debugger != null){
      when(call.method){
        MethodNames.show -> {
          AnalyticsDebuggerMethods.show(activity!!, debugger!!, call)
        }
        MethodNames.hide -> {
          AnalyticsDebuggerMethods.hide(activity!!, debugger!!)
        }
        MethodNames.send -> {
          AnalyticsDebuggerMethods.send(debugger!!, call)
        }
        else -> result.notImplemented()
      }
    }
    else {
      result.notImplemented()
    }
  }

  override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
    channel.setMethodCallHandler(null)
  }

  override fun onAttachedToActivity(binding: ActivityPluginBinding) {
    activity = binding.activity
    debugger =
      DebuggerManager()
  }

  override fun onDetachedFromActivityForConfigChanges() {
    activity = null
  }

  override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
    activity = binding.activity
    debugger =
      DebuggerManager()
  }

  override fun onDetachedFromActivity() {
    activity = null
  }
}
