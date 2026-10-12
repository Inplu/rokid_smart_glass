package com.rokid.glass

import com.rokid.glass.utils.demoString
import com.rokid.glesse.R

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.rokid.glass.utils.Scopes
import com.rokid.glass.utils.TimeUtils
import com.rokid.security.glass3.open.sdk.GlassSdk
import com.rokid.security.glass3.open.sdk.uitls.log.L
import com.rokid.security.glass3.sdk.base.data.offlineCmd.bean.VoiceAction
import com.rokid.security.glass3.sdk.base.data.offlineCmd.listener.IVoiceCallback
import com.rokid.security.system.server.media.callback.AudioCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AudioService : Service() {

    companion object {
        private const val TAG = "AudioService"
        private const val CHANNEL_ID = "audio_server_channel"
        private const val NOTIFICATION_ID = 1

        @Volatile
        private var isRunning = false

        // 供外部调用的查询方法
        // Query method exposed to external callers.
        fun isServiceRunning() = isRunning

        // 使用 AtomicReference 保证线程安全
        // Use AtomicReference to ensure thread safety.
        @Volatile
        private var instance: AudioService? = null

        // 获取单例实例
        // Get the singleton instance.
        fun getInstance(): AudioService? = instance

        // 启动服务的便捷方法
        // Convenience method for starting the service.
        fun start(context: Context) {
            val intent = Intent(context, AudioService::class.java)
            context.startForegroundService(intent)
        }

        // 停止服务的便捷方法
        // Convenience method for stopping the service.
        fun stop(context: Context) {
            val intent = Intent(context, AudioService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        instance = this
        // 启动前台服务
        // Start the foreground service.
        startForegroundService()
        // 记录启动时间
        // Record the start time.
        startTimeMillis = System.currentTimeMillis()
        Scopes.workScope.launch {
            while (isRunning) {
                delay(1000 * 15)
            }
        }
        qtVoiceAction = VoiceAction(demoString(R.string.demo_autumn_is_not_coming_back), "qiu tian bu hui lai", object : IVoiceCallback.Stub() {
            override fun onVoiceTriggered() {
                Log.e(TAG, "秋天不回来")
            }
        })
        GlassSdk.getGlassOfflineCmdService()?.add(qtVoiceAction)
    }

    private val audioCallback = object : AudioCallback.Stub() {
        override fun onAudioStream(buffer: ByteArray?, bufferLen: Int) {
            if (buffer == null) {
                return
            }
            L.d(TAG, "onAudioStream-->bufferLen: ${buffer.filter { it != 0.toByte() }.joinToString(" ")}")
        }

        override fun getCallbackId(): String {
            return "audio1"
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        // 1. 创建通知通道
        // 1. Create the notification channel.
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_LOW)
            .setName(demoString(R.string.demo_audio_service))
            .setDescription(demoString(R.string.demo_audio_service_notification))
            .build()
        NotificationManagerCompat.from(this).createNotificationChannel(channel)
        // 2. 创建通知
        // 2. Create the notification.
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(demoString(R.string.demo_audio_service_2))
            .setContentText(demoString(R.string.demo_running))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val serviceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
            // 3. 将服务提升为前台服务
            // 3. Promote the service to a foreground service.
            startForeground(NOTIFICATION_ID, notification, serviceType)
        } else {
            // 3. 将服务提升为前台服务
            // 3. Promote the service to a foreground service.
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @Volatile
    // 服务启动时间戳
    // Timestamp when the service started.
    private var startTimeMillis: Long = 0L
    private lateinit var qtVoiceAction: VoiceAction

    // 获取服务运行时长（毫秒）
    // Get the service uptime in milliseconds.
    fun getServiceRunTime(): Long {
        return if (isRunning && startTimeMillis > 0) {
            System.currentTimeMillis() - startTimeMillis
        } else {
            0L
        }
    }

    // 获取格式化的运行时长字符串
    // Get the formatted service uptime string.
    fun getRunTime(): String {
        val runTimeMillis = getServiceRunTime()
        return TimeUtils.formatDuration(runTimeMillis)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        L.d(TAG, "onStartCommand: 服务已启动")
        // 返回 START_STICKY 表示服务被终止后会自动重启
        // Return START_STICKY so the service restarts automatically after being terminated.
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        L.d(TAG, "onDestroy: 服务已停止")
        GlassSdk.getGlassMediaService()?.stopAudioRecord(audioCallback)
        if (::qtVoiceAction.isInitialized) {
            GlassSdk.getGlassOfflineCmdService()?.remove(qtVoiceAction)
        }
        isRunning = false
    }

    // Binder 类（保留但不再使用，可根据需要移除）
    // Binder class, retained but no longer used; remove it if unnecessary.
    inner class LocalBinder : Binder() {
        fun getService(): AudioService = this@AudioService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

}
