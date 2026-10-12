package com.rokid.glass

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class GlassesButtonReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            ORDER_ACTION_BUTTON_CLICK -> {
                Log.d("GlassesButton", "收到单击事件")
                // 在此处理镜腿单击事件。
                // Handle a single press of the temple button here.
                // 拦截广播，阻止系统默认处理
                // Intercept the broadcast to prevent the system's default handling.
                abortBroadcast()
            }
            ORDER_ACTION_BUTTON_DOUBLE_CLICK -> {
                Log.d("GlassesButton", "收到双击事件")
                // 在此处理镜腿双击事件。
                // Handle a double press of the temple button here.
                abortBroadcast()
            }
        }
    }

    companion object {
        const val ORDER_ACTION_BUTTON_CLICK =
            "com.rokid.glass3.action.button.CLICK"
        const val ORDER_ACTION_BUTTON_DOUBLE_CLICK =
            "com.rokid.glass3.action.button.DOUBLE_CLICK"
    }
}
