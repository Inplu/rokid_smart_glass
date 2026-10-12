package com.rokid.glass.utils

import android.os.Handler
import android.os.Looper
import android.util.Log

/**
 * 长点击事件检测器
 * Long-press event detector.
 * 用于检测指环按键的长按事件
 * Detects long-press events from ring buttons.
 * 自动检测长按开始和结束
 * Automatically detects when a long press starts and ends.
 */
class LongPressDetector private constructor(
    private val longPressDelay: Long = DEFAULT_LONG_PRESS_DELAY,
    private val autoStopDelay: Long = DEFAULT_AUTO_STOP_DELAY
) {

    companion object {
        private const val TAG = "LongPressDetector"
        // 默认250 毫秒触发长按
        // Trigger a long press after 250 milliseconds by default.
        private const val DEFAULT_LONG_PRESS_DELAY = 250L
        // 默认1000 毫秒自动判定为停止
        // Automatically treat the press as stopped after 1000 milliseconds by default.
        private const val DEFAULT_AUTO_STOP_DELAY = 1000L

        // 创建单例实例
        // Create the singleton instance.
        @Volatile
        private var instance: LongPressDetector? = null

        fun getInstance(
            longPressDelay: Long = DEFAULT_LONG_PRESS_DELAY,
            autoStopDelay: Long = DEFAULT_AUTO_STOP_DELAY
        ): LongPressDetector {
            return instance ?: synchronized(this) {
                instance ?: LongPressDetector(longPressDelay, autoStopDelay).also { instance = it }
            }
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var longPressJob: Runnable? = null
    private var autoStopJob: Runnable? = null
    private var isLongPressing = false
    private var lastKeyDownTime = 0L
    private var currentKeyCode = -1

    /**
     * 开始检测长按事件
     * Start detecting long-press events.
     * @param keyCode 按键码
     * The key code.
     * @param onLongPressStart 长按开始回调
     * Callback when a long press starts.
     * @param onLongPressEnd 长按结束回调
     * Callback when a long press ends.
     */
    fun startDetect(
        keyCode: Int,
        onLongPressStart: (() -> Unit)? = null,
        onLongPressEnd: (() -> Unit)? = null
    ) {
        val currentTime = System.currentTimeMillis()

        // 如果是同一个按键且已经在长按中，重置自动停止计时器
        // Reset the automatic stop timer if the same key is already being held.
        if (isLongPressing && keyCode == currentKeyCode) {
            Log.d(TAG, "同一组长按事件，重置计时器：keyCode=$keyCode")
            resetAutoStopTimer(keyCode, onLongPressEnd)
            return
        }

        // 取消之前的任务
        // Cancel the previous task.
        cancelDetect()

        lastKeyDownTime = currentTime
        currentKeyCode = keyCode

        // 创建长按任务
        // Create the long-press task.
        longPressJob = Runnable {
            isLongPressing = true
            onLongPressStart?.invoke()

            // 启动自动停止检测
            // Start automatic stop detection.
            startAutoStopDetection(currentKeyCode, onLongPressEnd)
        }

        // 延迟执行长按任务
        // Run the long-press task after a delay.
        handler.postDelayed(longPressJob!!, longPressDelay)
    }

    /**
     * 停止检测长按事件
     * Stop detecting long-press events.
     * @param onLongPressEnd 长按结束回调
     * Callback when a long press ends.
     */
    fun stopDetect(onLongPressEnd: (() -> Unit)? = null) {
        handleLongPressEnd(onLongPressEnd)
    }

    /**
     * 处理长按结束
     * Handle the end of a long press.
     */
    private fun handleLongPressEnd(onLongPressEnd: (() -> Unit)? = null) {
        if (isLongPressing) {
            onLongPressEnd?.invoke()
        }
        reset()
    }

    /**
     * 取消所有检测任务
     * Cancel all detection tasks.
     */
    private fun cancelDetect() {
        longPressJob?.let {
            handler.removeCallbacks(it)
            longPressJob = null
        }
        autoStopJob?.let {
            handler.removeCallbacks(it)
            autoStopJob = null
        }
    }

    /**
     * 启动自动停止检测
     * Start automatic stop detection.
     * 当超过设定时间没有再次收到按键事件时，自动判定为停止长按
     * Treat the long press as ended if no further key event arrives within the configured timeout.
     */
    private fun startAutoStopDetection(keyCode: Int, onLongPressEnd: (() -> Unit)? = null) {
        autoStopJob = Runnable {
            val elapsedTime = System.currentTimeMillis() - lastKeyDownTime
            if (elapsedTime >= autoStopDelay) {
                // 超过自动停止时间，判定为长按结束
                // The automatic stop timeout has elapsed, so the long press has ended.
                Log.d(TAG, "自动检测到长按停止：keyCode=$keyCode, 间隔=${elapsedTime}ms")
                handleLongPressEnd(onLongPressEnd)
            } else {
                // 继续检测
                // Continue detection.
                startAutoStopDetection(keyCode, onLongPressEnd)
            }
        }
        handler.postDelayed(autoStopJob!!, autoStopDelay)
    }

    /**
     * 重置自动停止计时器
     * Reset the automatic stop timer.
     * 当再次收到相同按键事件时调用
     * Call when another event for the same key arrives.
     */
    private fun resetAutoStopTimer(keyCode: Int, onLongPressEnd: (() -> Unit)? = null) {
        lastKeyDownTime = System.currentTimeMillis()
        autoStopJob?.let {
            handler.removeCallbacks(it)
            autoStopJob = null
        }
        // 重新启动检测
        // Restart detection.
        startAutoStopDetection(keyCode, onLongPressEnd)
    }

    /**
     * 重置检测器状态
     * Reset the detector state.
     */
    fun reset() {
        cancelDetect()
        isLongPressing = false
        lastKeyDownTime = 0L
        currentKeyCode = -1
    }

    /**
     * 是否正在长按中
     * Whether a long press is in progress.
     */
    fun isLongPressing(): Boolean {
        return isLongPressing
    }

    /**
     * 获取当前长按的按键码
     * Get the key code for the current long press.
     */
    fun getCurrentKeyCode(): Int {
        return currentKeyCode
    }
}
