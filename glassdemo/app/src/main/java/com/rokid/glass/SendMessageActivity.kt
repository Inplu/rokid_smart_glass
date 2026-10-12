package com.rokid.glass

import com.rokid.glass.utils.demoString

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.common.Barcode
import com.rokid.glass.audio.AudioStreamControl
import com.rokid.glass.audio.AudioStreamSession
import com.rokid.glass.base.BaseActivity
import com.rokid.glass.base.GlassKeyEvent
import com.rokid.glass.speech.OnlineAsrStatusMessages
import com.rokid.glass.utils.FileSizeUtil
import com.rokid.glass.utils.FileUtils
import com.rokid.glesse.R
import com.rokid.glesse.databinding.ActivitySendmessageBinding
import com.rokid.security.glass3.open.sdk.GlassSdk
import com.rokid.security.glass3.open.sdk.uitls.log.L
import com.rokid.security.glass3.qrcode.api.GlassScanCallback
import com.rokid.security.glass3.qrcode.api.GlassScanner
import com.rokid.security.glass3.qrcode.model.GlassScanConfig
import com.rokid.security.glass3.qrcode.model.ScanType
import com.rokid.security.glass3.sdk.base.data.device.bean.GlassAppConfig
import com.rokid.security.glass3.sdk.base.data.device.bean.GlassAppType
import com.rokid.security.glass3.sdk.base.data.device.bean.ThirdPartyApp
import com.rokid.security.glass3.sdk.base.data.device.wifi.WifiConnectRequest
import com.rokid.security.glass3.sdk.base.data.device.wifi.WifiNetworkInfo
import com.rokid.security.glass3.sdk.base.data.device.wifi.WifiOperationCode
import com.rokid.security.glass3.sdk.base.data.device.wifi.WifiOperationStage
import com.rokid.security.glass3.sdk.base.data.device.wifi.WifiRemoveRequest
import com.rokid.security.glass3.sdk.base.data.offlineCmd.bean.VoiceAction
import com.rokid.security.glass3.sdk.base.data.offlineCmd.listener.IVoiceCallback
import com.rokid.security.system.server.asr.listener.SpeechCallback
import com.rokid.security.system.server.device.listener.IAppVisibilityListener
import com.rokid.security.system.server.device.listener.IWifiOperationCallback
import com.rokid.security.system.server.message.callback.IResultCallback
import com.rokid.security.system.server.message.file.listener.FileReceiveListener
import com.rokid.security.system.server.message.listener.IMessageListener
import com.rokid.security.system.server.tts.ITtsService
import com.rokid.security.system.server.tts.listener.SpeechCompleteListener
import com.rokid.glass.speech.OnlineSpeechSession
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * 眼镜端 SDK 功能演示页面。
 * Demo page for glasses SDK features.
 *
 * 集中演示 TTS/ASR、P2P 与蓝牙消息、文件传输、音频流、相机共享、
 * Demonstrates TTS/ASR, P2P and Bluetooth messaging, file transfer, audio streams, camera sharing,
 * 二维码识别和应用可见性配置。页面通过眼镜前后键切换功能，点击键执行当前功能。
 * QR code recognition, and app visibility settings. Use the glasses' forward/back keys to select a feature and the click key to run it.
 */
class SendMessageActivity : BaseActivity() {

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 1001
        private val ONLINE_TTS_DEMO_TEXT: String get() = demoString(R.string.demo_this_is_an_online_text_to_speech_demo)
        private val OFFLINE_TTS_DEMO_TEXT: String get() = demoString(R.string.demo_this_is_an_offline_text_to_speech_demo)
        private const val ONLINE_ASR_TIMEOUT_MS = 15_000L
        private const val ONLINE_TTS_TIMEOUT_MS = 15_000L
        private const val DEMO_WIFI_SSID = "EBG-RD"
        private const val DEMO_WIFI_PASSWORD = "Rkd2023/"
        private const val DEMO_WIFI_HIDDEN = false
    }

    // 当前获得焦点的功能按钮 id，点击事件通过该 id 分发到对应 SDK 功能。
    // ID of the focused feature button; clicks use it to dispatch the corresponding SDK operation.
    private var currentSelectId: Int = -1

    // 文件发送时在两个示例文件之间交替选择。
    // Alternate between the two sample files when sending files.
    private var randomFile = false
    private val TAG = "SendMessageActivity"
    private lateinit var binding: ActivitySendmessageBinding

    // 记录文件开始发送时间，用于在完成回调中计算耗时。
    // Record the file transfer start time to calculate elapsed time in the completion callback.
    private var startTime = 0L

    // 眼镜按键菜单当前位置，对应 menuButtons() 中的下标。
    // Current position in the glasses key menu, corresponding to an index in menuButtons().
    private var selectBtnStatus = 0

    // Assets 中的示例文件会复制到公共 Download 目录，再用于传输测试。
    // Copy sample assets to the public Download directory for transfer testing.
    private val sdDownload = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS)

    private lateinit var huoVoiceAction: VoiceAction
    private var startAsrAfterPermissionGranted = false
    private var onlineAsrTimeoutJob: Job? = null
    private val onlineAsrSession = OnlineSpeechSession()
    private val onlineTtsSession = OnlineSpeechSession()
    private var onlineTtsTimeoutJob: Job? = null
    private var onlineTtsService: ITtsService? = null
    private var onlineTtsListener: SpeechCompleteListener? = null
    private val audioStreamSession = AudioStreamSession()
    private val gson = Gson()
    private val speechHandler = Handler(Looper.getMainLooper())
    private var ttsPageActive = false

    private val audioTrack = AudioTrack(
        // 音频流类型
        // Audio stream type.
        AudioManager.STREAM_MUSIC,
        // 采样率（必须一致）
        // Sample rate, which must match.
        16000,
        // 单声道（与录音一致）
        // Mono audio, matching the recording configuration.
        AudioFormat.CHANNEL_OUT_MONO,
        // 编码格式（必须一致）
        // Encoding format, which must match.
        AudioFormat.ENCODING_PCM_16BIT,
        // 合理的缓冲区大小
        // An appropriate buffer size.
        AudioTrack.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ),
        // 流式模式（适合实时播放）
        // Streaming mode, suitable for real-time playback.
        AudioTrack.MODE_STREAM
    )

    private fun deliver(session: OnlineSpeechSession, token: Long, action: () -> Unit) {
        speechHandler.post {
            if (session.accepts(token) && !isDestroyed) action()
        }
    }

    private fun speechCallback(token: Long) = object : SpeechCallback.Stub() {
        override fun onStart() = deliver(onlineAsrSession, token) {
            onlineAsrSession.started(token)
            cancelOnlineAsrTimeout()
            log(OnlineAsrStatusMessages.started)
        }

        override fun onIntermediateVad(content: String) = deliver(onlineAsrSession, token) { log(content) }

        override fun onAsrComplete(content: String?) = deliver(onlineAsrSession, token) {
            val result = content.orEmpty()
            log(if (result.isBlank()) demoString(R.string.demo_speech_recognition_finished_with_no_text) else result)
        }

        override fun onAsrCompleteWithIntent(content: String?, intent: Int, intentJson: String) = deliver(onlineAsrSession, token) {
            log(demoString(R.string.demo_recognized_text_intent_index_intent_json, content.orEmpty(), intent, intentJson))
        }

        override fun onError(code: Int) = deliver(onlineAsrSession, token) {
            stopOnlineAsr()
            log(OnlineAsrStatusMessages.error(code))
        }

        override fun onServiceConnectState(connect: Boolean) = deliver(onlineAsrSession, token) {
            // SDK 2.2.0-E replays cached state and supplies no route/session ID.
            // This signal alone cannot establish that this recognition request failed.
            if (connect) log(OnlineAsrStatusMessages.connected)
            else log(demoString(R.string.demo_online_asr_connection_is_not_ready_waiting_for))
        }
    }

    // 标记下一次操作是隐藏指定应用，还是恢复默认应用可见性。
    // Whether the next operation hides specified apps or restores default app visibility.
    private var isHide = true

    /**
     * 获取P2P发送文件的管理器
     * Get the manager for sending files over P2P.
     * @return IGlassFileOperate
     */
    private val mFileOperator by lazy {
        GlassSdk.getGlassMessageService()?.glassFileOperater
    }

    /**
     * 获取蓝牙发送文件的管理器
     * Get the manager for sending files over Bluetooth.
     * @return IGlassFileOperate
     */
    private val mBTFileOperator by lazy {
        GlassSdk.getGlassMessageService()?.glassBtFileOperater
    }

    /**
     * 蓝牙文件发送状态回调。
     * Bluetooth file transfer status callback.
     *
     * FileReceiveListener 名称虽然是 ReceiveListener，但发送方也通过它接收
     * Despite its name, FileReceiveListener is also used by the sender to receive
     * 开始、进度、完成、失败和取消等传输状态。
     * transfer states such as start, progress, completion, failure, and cancellation.
     */
    private val bleFileReceiveListener = object : FileReceiveListener.Stub() {
        override fun onStart() {
            startTime = System.currentTimeMillis()
            Log.i(TAG, "onStart: 蓝牙本端开始发送文件")
            log(demoString(R.string.demo_started_sending_file_via_bluetooth))
        }

        override fun onProgressChanged(progress: Float) {
            Log.i(TAG, "onProgressChanged: 蓝牙本端发送文件的进度 $progress")
            log(demoString(R.string.demo_bluetooth_file_transfer_progress, progress))
        }

        /**
         * 眼镜端待发送文件的路径
         * Path of the file to send from the glasses.
         */
        override fun onComplete(filePath: String) {
            // 手机端接收文件的路径 /storage/emulated/0/Android/data/com.rokid.phone/files/receiver/gonglu.png
            // Received file path on the phone: /storage/emulated/0/Android/data/com.rokid.phone/files/receiver/gonglu.png
            Log.i(TAG, "onComplete: 蓝牙本端发送文件完成,----$filePath")
            var duration = System.currentTimeMillis() - startTime
            val fileSize = FileSizeUtil.formatFileSize(FileSizeUtil.getFileSizeBytes(curFile).toDouble())
            if (duration > 1000) {
                duration = duration / 1000
                log(demoString(R.string.demo_bluetooth_file_sent_in_seconds, duration, fileSize))
            } else {
                log(demoString(R.string.demo_bluetooth_file_sent_in_ms, duration, fileSize))
            }
        }

        override fun onFail() {
            Log.i(TAG, "onFail: 蓝牙本端发送文件失败")
            log(demoString(R.string.demo_failed_to_send_file_via_bluetooth))
        }

        override fun onCancel() {
            Log.i(TAG, "onCancel: 蓝牙对方取消了发送文件")
            log(demoString(R.string.demo_bluetooth_recipient_canceled_file_transfer))
        }
    }

    /**
     * P2P 文件发送状态回调，负责输出进度并统计发送耗时和文件大小。
     * P2P file transfer status callback; report progress and track transfer duration and file size.
     */
    private val p2pFileReceiveListener = object : FileReceiveListener.Stub() {
        override fun onStart() {
            startTime = System.currentTimeMillis()
            Log.i(TAG, "onStart: p2p本端开始发送文件")
            log(demoString(R.string.demo_started_sending_file_via_p2p))
        }

        override fun onProgressChanged(progress: Float) {
            Log.i(TAG, "onProgressChanged: p2p本端发送文件的进度 $progress")
            log(demoString(R.string.demo_p2p_file_transfer_progress, progress))
        }

        /**
         * 眼镜端待发送文件的路径
         * Path of the file to send from the glasses.
         */
        override fun onComplete(filePath: String) {
            // 手机端接收文件的路径 /storage/emulated/0/Android/data/com.rokid.phone/files/receiver/gonglu.png
            // Received file path on the phone: /storage/emulated/0/Android/data/com.rokid.phone/files/receiver/gonglu.png
            Log.i(TAG, "onComplete: p2p本端发送文件完成,----$filePath")
            var duration = System.currentTimeMillis() - startTime
            val fileSize = FileSizeUtil.formatFileSize(FileSizeUtil.getFileSizeBytes(curFile).toDouble())
            if (duration > 1000) {
                duration = duration / 1000
                log(demoString(R.string.demo_p2p_file_sent_in_seconds, duration, fileSize))
            } else {
                log(demoString(R.string.demo_p2p_file_sent_in_ms, duration, fileSize))
            }
        }

        override fun onFail() {
            Log.i(TAG, "onFail: p2p本端发送文件失败")
            log(demoString(R.string.demo_failed_to_send_file_via_p2p))
        }

        override fun onCancel() {
            Log.i(TAG, "onCancel: p2p对方取消了发送文件")
            log(demoString(R.string.demo_p2p_recipient_canceled_file_transfer))
        }
    }

    val a1File = "xiyi.jpg"
    val a2File = "gonglu.png"

    // 当前正在发送的文件，完成回调通过它统计源文件大小。
    // The file currently being sent; the completion callback uses it to determine the source file size.
    var curFile = File("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e(TAG, "初始化")
        binding = ActivitySendmessageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 提前准备文件传输演示使用的两个本地文件。
        // Prepare the two local files used by the file transfer demo in advance.
        copyFileFromAssetsToExternalStorage(this, a1File)
        copyFileFromAssetsToExternalStorage(this, a2File)

        // 默认选中第一个功能，确保眼镜点击键进入页面后可以直接操作。
        // Select the first feature by default so the glasses click key works as soon as the page opens.
        selectBtn(binding.btnOnlineTts)

        // 分别注册经典蓝牙和 P2P 文件传输状态监听。
        // Register file transfer status listeners for classic Bluetooth and P2P separately.
        mBTFileOperator?.setFileReceiveListener(bleFileReceiveListener)
        mFileOperator?.setFileReceiveListener(p2pFileReceiveListener)

        // 注册一条离线语音命令，页面销毁时会解除注册。
        // Register an offline voice command and unregister it when the page is destroyed.
        huoVoiceAction = VoiceAction(demoString(R.string.demo_hazard_detected), "fa xian yin huan", object : IVoiceCallback.Stub() {
            override fun onVoiceTriggered() {
                Log.e(TAG, "发现隐患")
            }
        })
        GlassSdk.getGlassOfflineCmdService()?.add(huoVoiceAction)

        // 同时兼容键盘和眼镜触控板映射的 Enter/左右方向键。
        // Support Enter and left/right arrow events from both keyboards and the glasses touchpad.
        binding.keyMark.setOnKeyListener { view, keyCode, event ->
            when (event.action) {
                KeyEvent.ACTION_DOWN -> {
                    when (event.keyCode) {
                        KeyEvent.KEYCODE_ENTER -> {
                            Log.d(TAG, "setOnKeyListener 键盘回车事件或者触摸板点击事件")
                            toClick()
                        }

                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            Log.d(TAG, "setOnKeyListener 键盘往前键")
                            onGlassKeyEvent(GlassKeyEvent.KEYCODE_FRONT)
                        }

                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            Log.d(TAG, "setOnKeyListener 键盘往后键")
                            onGlassKeyEvent(GlassKeyEvent.KEYCODE_BEHIND)
                        }

                        KeyEvent.KEYCODE_BACK -> {
                            Log.d(TAG, "setOnKeyListener 鼠标右击返回事件")
                            finish()
                        }
                    }
                }
            }
            true
        }


        GlassSdk.getGlassMessageService()?.setMessageListener(mMessageListener)

    }

    /**
     * 执行当前选中菜单项。
     * Execute the currently selected menu item.
     *
     * 该方法只负责按按钮 id 分发功能；眼镜点击键和键盘 Enter 最终都会进入这里。
     * This method only dispatches actions by button ID; both the glasses click key and keyboard Enter eventually reach it.
     */
    private fun toClick() {
        when (currentSelectId) {
            R.id.btnOnlineTts -> playOnlineTts()

            R.id.btnOfflineTts -> playOfflineTts()

            R.id.btnOnlineAsr -> startAsr()

            R.id.wfSendTextBtn -> {
                GlassSdk.getGlassMessageService()?.sendTextMessageByP2P(demoString(R.string.demo_p2p_test_message_from_glasses))
                log(demoString(R.string.demo_p2p_test_message_from_glasses))
            }

            R.id.wfSendFileBtn -> {
                val fileToSend1 = File(sdDownload, a1File)
                val fileToSend2 = File(sdDownload, a2File)
                logBuilder.clear()
                log(demoString(R.string.demo_preparing_to_send_file_via_p2p))
                // 连续操作时交替发送两个文件，方便验证不同大小/格式的传输。
                // Alternate between the two files on successive operations to verify transfers of different sizes and formats.
                if (randomFile) {
                    randomFile = false
                    Log.e(TAG, "文件目录: ${fileToSend1.absolutePath}")
                    curFile = fileToSend1
                    mFileOperator?.sendFile("", fileToSend1.absolutePath, p2pFileReceiveListener, mResultCallback)
                } else {
                    randomFile = true
                    Log.e(TAG, "文件目录: ${fileToSend2.absolutePath}")
                    curFile = fileToSend2
                    mFileOperator?.sendFile("", fileToSend2.absolutePath, p2pFileReceiveListener, mResultCallback)
                }
            }

            R.id.btSendTextBtn -> {
                GlassSdk.getGlassMessageService()?.sendTextMessageByClassicBT(demoString(R.string.demo_bluetooth_test_message_from_glasses))
                log(demoString(R.string.demo_bluetooth_test_message_from_glasses))
            }

            R.id.btSendFileBtn -> {
                val fileToSend1 = File(sdDownload, a1File)
                val fileToSend2 = File(sdDownload, a2File)
                logBuilder.clear()
                log(demoString(R.string.demo_preparing_to_send_file_via_bluetooth))
                // 与 P2P 测试保持一致，交替发送两个 Assets 示例文件。
                // As in the P2P test, alternate between the two sample assets.
                if (randomFile) {
                    randomFile = false
                    curFile = fileToSend1
                    Log.e(TAG, "蓝牙发送---->文件目录: ${fileToSend1.absolutePath}")
                    mBTFileOperator?.sendFile("", fileToSend1.absolutePath, bleFileReceiveListener, mResultCallback)
                } else {
                    randomFile = true
                    curFile = fileToSend2
                    Log.e(TAG, "蓝牙发送---->文件目录: ${fileToSend2.absolutePath}")
                    mBTFileOperator?.sendFile("", fileToSend2.absolutePath, bleFileReceiveListener, mResultCallback)
                }
            }


            R.id.btSendAudioStream -> {
                startAudioStreamRemotely()
            }

            R.id.btStopSendAudioStream -> {
                stopAudioStreamLocally(notifyPhone = true)
            }

            R.id.btCameraShare -> {
                val intent = Intent(this, CameraShareSelectActivity::class.java)
                startActivity(intent)
            }

            R.id.btQRCodeImage -> {
                val bitmap = FileUtils.loadBitmapFromAssets(this, "qrcode.jpg") ?: run {
                    log(demoString(R.string.demo_failed_to_load_qr_code_image))
                    return
                }
                GlassScanner.scanFromBitmap(this, bitmap = bitmap, callback = object : GlassScanCallback {
                    override fun onScanFailure(error: String) {
                        log(demoString(R.string.demo_scan_failed, error))
                    }

                    override fun onScanSuccess(content: String?, barcode: Barcode) {
                        log(demoString(R.string.demo_scan_successful_barcode, content, barcode.rawValue))
                    }
                })
            }

            R.id.btQRCode -> {
                // cameraZoomLevel 取值范围为 1～10；适当放大可提升远距离二维码的识别效果。
                // cameraZoomLevel ranges from 1 to 10; moderate zoom can improve recognition of distant QR codes.
                GlassScanner.launch(
                    this,
                    config = GlassScanConfig(enableAutoClose = true, scanType = ScanType.QR_CODE_ONLY, cameraZoomLevel = 5),
                    scanCallback = object : GlassScanCallback {
                        override fun onScanSuccess(content: String?, barcode: Barcode) {
                            log(demoString(R.string.demo_scan_successful, content))
                        }

                        override fun onScanFailure(error: String) {
                            log(demoString(R.string.demo_scan_failed, error))
                        }
                    })
            }

            R.id.btQRCodeXml -> {
                startActivity(Intent(this, ActivityQRView::class.java))
            }

            R.id.btConnectWifi -> {
                connectWifi()
            }

            R.id.btDisconnectWifi -> {
                disconnectWifi()
            }

            R.id.btConfigureAppVisibility -> {
                // 第一次点击隐藏指定系统应用并把三方应用排到最前面
                // The first click hides specified system apps and moves third-party apps to the front.
                // 再次点击传入空配置，恢复设备默认的应用可见性。
                // The next click passes an empty configuration to restore the device's default app visibility.
                if (isHide) {
                    val appList = mutableListOf(
                        GlassAppType.AI_WORK_ASSISTANT, GlassAppType.AI_CHAT,
                        GlassAppType.AI_INSPECTION, GlassAppType.OFFLINE_FACE,
                        GlassAppType.TAKE_PHOTO, GlassAppType.XPERT,
                        GlassAppType.OFFLINE_PLATE, GlassAppType.HG_IDENTIFICATION
                    )
                    // 只要写了三方应用就会排到最前面
                    // Any specified third-party app is placed at the front.
                    val thirdList = mutableListOf(ThirdPartyApp("com.rokid.glesse", "glassdemo"))
                    GlassSdk.getGlassDeviceService()
                        ?.configureAppVisibility(GlassAppConfig(appList, thirdList), object : IAppVisibilityListener.Stub() {
                            override fun onResult(success: Boolean) {
                                L.d(TAG, "--------setAppVisibility=${success}")
                            }
                        })
                } else {
                    GlassSdk.getGlassDeviceService()
                        ?.configureAppVisibility(GlassAppConfig(), object : IAppVisibilityListener.Stub() {
                            override fun onResult(success: Boolean) {
                                L.d(TAG, "--------setAppVisibility=${success}")
                            }
                        })
                }
                isHide = !isHide
            }
        }
    }

    /**
     * 连接指定 Wi-Fi。
     * Connect to the specified Wi-Fi network.
     *
     * 修改 companion object 中的 DEMO_WIFI_SSID / DEMO_WIFI_PASSWORD 即可切换测试热点。
     * Change DEMO_WIFI_SSID / DEMO_WIFI_PASSWORD in the companion object to switch the test access point.
     * 空密码会按开放网络处理；非空密码默认使用自动 PSK 模式。
     * An empty password selects an open network; a non-empty password uses automatic PSK mode by default.
     */
    private fun connectWifi() {
        if (!checkDeviceServiceReady()) {
            return
        }

        val ssid = DEMO_WIFI_SSID.trim()
        if (ssid.isBlank()) {
            log(demoString(R.string.demo_configure_the_wi_fi_network_name_first))
            return
        }

        val currentWifiInfo = GlassSdk.getCurrentWifiInfo()
        if (currentWifiInfo?.status == WifiNetworkInfo.STATUS_CURRENT &&
            !currentWifiInfo.ssid.isNullOrBlank() && currentWifiInfo.ssid == ssid
        ) {
            log(demoString(R.string.demo_already_connected_to_wi_fi, currentWifiInfo.ssid))
            return
        }

        val password = DEMO_WIFI_PASSWORD.takeIf { it.isNotBlank() }
        val request = WifiConnectRequest(
            ssid = ssid,
            securityType = if (password == null) {
                WifiConnectRequest.SECURITY_OPEN
            } else {
                WifiConnectRequest.SECURITY_WPA_PSK
            },
            password = password,
            hiddenSsid = DEMO_WIFI_HIDDEN
        )
        log(demoString(R.string.demo_connecting_to_wi_fi, ssid))
        GlassSdk.connectWifi(request, createWifiOperationCallback(demoString(R.string.demo_connect_wi_fi)))
    }

    /**
     * 断开当前 Wi-Fi。
     * Disconnect from the current Wi-Fi network.
     *
     * SDK 当前提供的是 removeWifi 接口，这里传 disconnectIfCurrent=true，
     * The SDK currently provides removeWifi; pass disconnectIfCurrent=true here
     * 会先断开当前连接，再移除对应的已保存配置。
     * to disconnect the current connection first, then remove its saved configuration.
     */
    private fun disconnectWifi() {
        if (!checkDeviceServiceReady()) {
            return
        }

        val currentWifiInfo = GlassSdk.getCurrentWifiInfo()
        if (currentWifiInfo?.ssid.isNullOrBlank()) {
            log(demoString(R.string.demo_no_wi_fi_network_connected_or_connecting))
            return
        }

        val request = WifiRemoveRequest(
            ssid = currentWifiInfo.ssid,
            bssid = currentWifiInfo.bssid,
            networkId = currentWifiInfo.networkId,
            disconnectIfCurrent = true
        )
        log(demoString(R.string.demo_disconnecting_and_removing_wi_fi, currentWifiInfo.ssid))
        GlassSdk.removeWifi(request, createWifiOperationCallback(demoString(R.string.demo_disconnect_wi_fi)))
    }

    private fun checkDeviceServiceReady(): Boolean {
        if (!GlassSdk.isReady()) {
            log(demoString(R.string.demo_sdk_initialization_is_incomplete_try_again_later))
            return false
        }
        if (GlassSdk.getGlassDeviceService() == null) {
            log(demoString(R.string.demo_device_service_unavailable_check_the_glasses_system_service))
            return false
        }
        return true
    }

    private fun createWifiOperationCallback(operationName: String) = object : IWifiOperationCallback.Stub() {
        override fun onProgress(stage: Int, message: String?) {
            val stageName = wifiStageName(stage)
            val detail = message.orEmpty().takeIf { it.isNotBlank() && it != stageName }
            log(if (detail == null) demoString(R.string.demo_progress, operationName, stageName) else demoString(R.string.demo_progress_2, operationName, stageName, detail))
        }

        override fun onResult(code: Int, message: String?, networkId: Int, ssid: String?) {
            if (code == WifiOperationCode.SUCCESS) {
                log(demoString(R.string.demo_succeeded_ssid_network_id, operationName, ssid.orEmpty(), networkId))
            } else {
                log(demoString(R.string.demo_failed_code_message, operationName, code, wifiCodeName(code), message.orEmpty()))
            }
        }
    }

    private fun wifiStageName(stage: Int): String {
        return when (stage) {
            WifiOperationStage.VALIDATING -> demoString(R.string.demo_validate_parameters)
            WifiOperationStage.CONFIGURING -> demoString(R.string.demo_configure_network)
            WifiOperationStage.CONNECTING -> demoString(R.string.demo_wait_for_connection)
            WifiOperationStage.VERIFYING -> demoString(R.string.demo_verify_connection)
            WifiOperationStage.REMOVING -> demoString(R.string.demo_remove_configuration)
            else -> demoString(R.string.demo_unknown_stage)
        }
    }

    private fun wifiCodeName(code: Int): String {
        return when (code) {
            WifiOperationCode.SUCCESS -> demoString(R.string.demo_success)
            WifiOperationCode.ERROR_INVALID_ARGUMENT -> demoString(R.string.demo_invalid_parameters)
            WifiOperationCode.ERROR_PERMISSION_DENIED -> demoString(R.string.demo_permission_denied)
            WifiOperationCode.ERROR_UNSUPPORTED_SECURITY -> demoString(R.string.demo_unsupported_security_type)
            WifiOperationCode.ERROR_WIFI_DISABLED -> demoString(R.string.demo_wi_fi_is_off)
            WifiOperationCode.ERROR_ADD_OR_UPDATE_FAILED -> demoString(R.string.demo_configuration_failed)
            WifiOperationCode.ERROR_AUTH_FAILED -> demoString(R.string.demo_authentication_failed)
            WifiOperationCode.ERROR_TIMEOUT -> demoString(R.string.demo_connection_timed_out)
            WifiOperationCode.ERROR_NOT_FOUND -> demoString(R.string.demo_configuration_not_found)
            WifiOperationCode.ERROR_INTERNAL -> demoString(R.string.demo_internal_error)
            else -> demoString(R.string.demo_unknown_error)
        }
    }

    private val mMessageListener = object : IMessageListener.Stub() {
        override fun onTextMessage(msg: String) {
            log(msg)
        }

        override fun onAudioStream(buffer: ByteArray) {
            log(demoString(R.string.demo_audio_stream_received_bytes, buffer.size))
            audioTrack.write(buffer, 0, buffer.size)
        }

        override fun onStreamDataReceived(tag: String, data: ByteArray) {
            log(demoString(R.string.demo_onstreamdatareceived_tag_status_26, data.size, tag))
        }
    }

    private fun startAsr() {
        if (onlineAsrSession.id != 0L) {
            val stopped = stopOnlineAsr()
            log(if (stopped) demoString(R.string.demo_online_asr_stop_requested_tap_again_to_start) else demoString(R.string.demo_failed_to_stop_online_asr_tap_again_to))
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            startAsrAfterPermissionGranted = true
            log(demoString(R.string.demo_microphone_permission_is_required_for_speech_recognition))
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
            return
        }

        if (!GlassSdk.isReady()) {
            log(demoString(R.string.demo_sdk_initialization_is_incomplete_try_again_later))
            return
        }

        if (!checkSpeechNetwork(demoString(R.string.demo_online_asr))) return

        val asrService = GlassSdk.getGlassAsrService()
        if (asrService == null) {
            log(OnlineAsrStatusMessages.serviceUnavailable)
            return
        }

        try {
            val token = onlineAsrSession.begin()
            binding.btnOnlineAsr.text = demoString(R.string.demo_stop_asr)
            log(OnlineAsrStatusMessages.connecting)
            scheduleOnlineAsrTimeout()
            asrService.startSpeech(speechCallback(token))
        } catch (error: SecurityException) {
            stopOnlineAsr()
            log(demoString(R.string.demo_online_asr_authentication_or_permission_failed_check_lingmou))
        } catch (error: Exception) {
            stopOnlineAsr()
            log(demoString(R.string.demo_failed_to_start_online_asr_check_the_network, error.message.orEmpty()))
        }
    }

    private fun scheduleOnlineAsrTimeout() {
        cancelOnlineAsrTimeout()
        onlineAsrTimeoutJob = lifecycleScope.launch {
            delay(ONLINE_ASR_TIMEOUT_MS)
            onlineAsrTimeoutJob = null
            if (onlineAsrSession.starting) {
                stopOnlineAsr()
                log(OnlineAsrStatusMessages.connectionTimeout)
            }
        }
    }

    private fun cancelOnlineAsrTimeout() {
        onlineAsrTimeoutJob?.cancel()
        onlineAsrTimeoutJob = null
    }

    private fun stopOnlineAsr(): Boolean {
        cancelOnlineAsrTimeout()
        val stopped = onlineAsrSession.cancel {
            runCatching {
                val service = GlassSdk.getGlassAsrService() ?: return@runCatching false
                service.stopSpeech()
                true
            }.getOrDefault(false)
        }
        binding.btnOnlineAsr.text = if (stopped) demoString(R.string.demo_online_asr_2) else demoString(R.string.demo_retry_stopping_asr)
        return stopped
    }

    private fun playOnlineTts() {
        if (onlineTtsSession.stopping && !cancelOnlineTts()) {
            return log(demoString(R.string.demo_previous_playback_cleanup_failed_queue_paused_try_again))
        }
        if (!GlassSdk.isReady()) return log(demoString(R.string.demo_sdk_initialization_is_incomplete_try_again_later))
        if (!checkSpeechNetwork(demoString(R.string.demo_online_tts))) return
        if (!onlineTtsSession.enqueue()) {
            return log(demoString(R.string.demo_online_tts_queue_is_full_up_to_requests, OnlineSpeechSession.CAPACITY))
        }
        if (onlineTtsSession.id != 0L) {
            log(demoString(R.string.demo_online_tts_queued_requests_ahead, onlineTtsSession.waiting))
        } else {
            dispatchNextOnlineTts()
        }
    }

    private fun dispatchNextOnlineTts() {
        if (!ttsPageActive || isFinishing || isDestroyed || onlineTtsSession.id != 0L || onlineTtsSession.waiting == 0) return
        if (!GlassSdk.isReady() || !checkSpeechNetwork(demoString(R.string.demo_online_tts))) {
            val removed = onlineTtsSession.clearQueue()
            return log(demoString(R.string.demo_speech_service_or_network_unavailable_canceled_queued_requests, removed))
        }
        val service = GlassSdk.getGlassTtsService() ?: run {
            val removed = onlineTtsSession.clearQueue()
            return log(demoString(R.string.demo_online_tts_service_unavailable_canceled_queued_requests, removed))
        }
        val token = onlineTtsSession.beginNext()
        if (token == 0L) return
        var submitted = false
        val listener = object : SpeechCompleteListener.Stub() {
            override fun onComplete() = deliver(onlineTtsSession, token) {
                onlineTtsSession.complete(token)
                onlineTtsTimeoutJob?.cancel()
                onlineTtsTimeoutJob = null
                detachOnlineTtsListener()
                log(demoString(R.string.demo_online_tts_playback_complete_remaining, onlineTtsSession.waiting))
                speechHandler.post { dispatchNextOnlineTts() }
            }

            override fun onServiceConnectState(connected: Boolean) = deliver(onlineTtsSession, token) {
                // Legacy state has no route identity; never cancel playback based on it alone.
                if (connected) log(demoString(R.string.demo_online_tts_service_connected))
                else log(demoString(R.string.demo_online_tts_connection_is_not_ready_waiting_for))
            }
        }
        onlineTtsService = service
        onlineTtsListener = listener
        try {
            service.setSpeechCompleteListener(listener)
            // Retain ownership before submission, including a lost IPC reply.
            submitted = true
            service.doSpeechTts(ONLINE_TTS_DEMO_TEXT)
            log(demoString(R.string.demo_online_tts_request_sent))
            onlineTtsTimeoutJob = lifecycleScope.launch {
                delay(ONLINE_TTS_TIMEOUT_MS)
                if (onlineTtsSession.accepts(token)) {
                    val cleaned = cancelOnlineTts()
                    log(if (cleaned) demoString(R.string.demo_no_online_tts_completion_callback_cancellation_requested_continuing)
                        else demoString(R.string.demo_online_tts_timed_out_and_cleanup_is_incomplete))
                    if (cleaned) speechHandler.post { dispatchNextOnlineTts() }
                }
            }
        } catch (error: Exception) {
            // Registration failure never submitted audio; do not invoke global cancel then.
            if (!submitted) onlineTtsSession.complete(token)
            val cleaned = cancelOnlineTts()
            log(describeOnlineTtsFailure(error))
            val removed = onlineTtsSession.clearQueue()
            log(demoString(R.string.demo_online_tts_request_failed_canceled_queued_requests, removed))
            if (!cleaned) log(demoString(R.string.demo_online_tts_cleanup_is_incomplete_try_again_later))
        }
    }

    override fun onStart() {
        super.onStart()
        ttsPageActive = true
    }

    private fun cancelOnlineTts(): Boolean {
        onlineTtsTimeoutJob?.cancel()
        onlineTtsTimeoutJob = null
        // cancel() rejects callbacks before invoking the SDK, even when cancellation fails.
        val canceled = onlineTtsSession.cancel {
            runCatching {
                val service = onlineTtsService ?: return@runCatching false
                // SDK 2.2.0-E exposes only global cancellation. Never invoke it while idle.
                service.doCancelTts()
                true
            }.getOrDefault(false)
        }
        detachOnlineTtsListener()
        return canceled
    }

    private fun detachOnlineTtsListener() {
        val listener = onlineTtsListener
        onlineTtsListener = null
        if (listener != null) runCatching { onlineTtsService?.removeSpeechCompleteListener() }
    }

    override fun onStop() {
        stopOnlineAsr()
        ttsPageActive = false
        val removed = onlineTtsSession.clearQueue()
        if (removed > 0 || onlineTtsSession.id != 0L) {
            log(demoString(R.string.demo_page_closed_canceled_current_playback_and_queued_requests, removed))
        }
        val cleaned = cancelOnlineTts()
        if (!cleaned) L.w("SendMessageActivity", "Online TTS cleanup failed on page stop")
        super.onStop()
    }

    private fun playOfflineTts() {
        if (!GlassSdk.isReady()) {
            return log(demoString(R.string.demo_sdk_initialization_is_incomplete_try_again_later))
        }
        val service = GlassSdk.getGlassOfflineTtsService()
            ?: return log(demoString(R.string.demo_offline_tts_service_unavailable))
        try {
            service.playTtsMsg(OFFLINE_TTS_DEMO_TEXT)
            log(demoString(R.string.demo_offline_tts_request_sent, OFFLINE_TTS_DEMO_TEXT))
        } catch (error: SecurityException) {
            log(demoString(R.string.demo_offline_tts_permission_denied_check_system_authorization))
        } catch (error: Exception) {
            log(demoString(R.string.demo_offline_tts_playback_failed, error.message ?: error.javaClass.simpleName))
        }
    }

    private fun checkSpeechNetwork(feature: String): Boolean {
        val route = runCatching { GlassSdk.getGlassDeviceService()?.getNetworkType() }.getOrNull()
        // Unknown Binder/network state must not be mistaken for a confirmed offline route.
        val localInternet: Boolean? = if (route == 1) runCatching {
            val cm = getSystemService(ConnectivityManager::class.java)
            val network = cm?.activeNetwork
            if (cm == null) null
            else if (network == null) false
            else cm.getNetworkCapabilities(network)
                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }.getOrNull() else null
        val policy = com.rokid.glass.speech.SpeechNetworkPolicy
        log(demoString(R.string.demo__status_27, feature, policy.hint(route)))
        if (!policy.canAttempt(route, localInternet)) {
            log(demoString(R.string.demo_no_network_available_in_direct_mode_connect_glasses, feature))
            return false
        }
        return true
    }

    private fun describeOnlineTtsFailure(error: Exception): String {
        val message = error.message.orEmpty()
        val normalized = message.lowercase()
        return when {
            listOf("401", "403", "auth", "token", "api key", "apikey", "unauthorized", "forbidden")
                .any(normalized::contains) ->
                demoString(R.string.demo_online_tts_authentication_or_permission_failed_check_lingmou)
            listOf("network", "timeout", "connect", "socket", "host", "dns")
                .any(normalized::contains) ->
                demoString(R.string.demo_online_tts_network_error, message.ifBlank { error.javaClass.simpleName })
            else -> demoString(R.string.demo_online_tts_playback_failed, message.ifBlank { error.javaClass.simpleName })
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_RECORD_AUDIO_PERMISSION) {
            return
        }

        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        if (granted && startAsrAfterPermissionGranted) {
            startAsrAfterPermissionGranted = false
            startAsr()
        } else {
            startAsrAfterPermissionGranted = false
            log(demoString(R.string.demo_microphone_permission_denied_speech_recognition_is_unavailable))
        }
    }

    /**
     * 先给 keyMark 处理
     * Let keyMark handle the event first.
     * 如果没处理
     * If it does not handle the event,
     * 系统继续处理
     * the system continues handling it.
     *
     *
     * dispatchKeyEvent	事件分发
     * dispatchKeyEvent: event dispatch.
     * onKeyDown	事件处理
     * onKeyDown: event handling.
     * onKeyUp	按键释放
     * onKeyUp: key release.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (binding.keyMark.dispatchKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onGlassKeyEvent(keyEvent: Int): Boolean {
        when (keyEvent) {
            GlassKeyEvent.KEYCODE_FRONT -> {
                moveSelect(1)
            }

            GlassKeyEvent.KEYCODE_BEHIND -> {
                moveSelect(-1)
            }

            GlassKeyEvent.KEYCODE_CLICK -> {
                // 点击键执行当前焦点对应的功能。
                // The click key executes the feature corresponding to the current focus.
                toClick()
            }
        }
        return super.onGlassKeyEvent(keyEvent)
    }

    private fun menuButtons(): List<AppCompatTextView> {
        return listOf(
            binding.btnOnlineTts,
            binding.btnOfflineTts,
            binding.btnOnlineAsr,
            binding.wfSendTextBtn,
            binding.wfSendFileBtn,
            binding.btSendTextBtn,
            binding.btSendFileBtn,
            binding.btSendAudioStream,
            binding.btStopSendAudioStream,
            binding.btCameraShare,
            binding.btQRCodeImage,
            binding.btQRCode,
            binding.btQRCodeXml,
            binding.btConnectWifi,
            binding.btDisconnectWifi,
            binding.btConfigureAppVisibility
        )
    }

    private fun moveSelect(step: Int) {
        val buttons = menuButtons()
        if (buttons.isEmpty()) {
            return
        }
        val oldSelectIndex = selectBtnStatus.coerceIn(0, buttons.lastIndex)
        unSelectBtn(buttons[oldSelectIndex])
        selectBtnStatus = (oldSelectIndex + step + buttons.size) % buttons.size
        selectBtn(buttons[selectBtnStatus])
    }

    /**
     * 清除按钮焦点并恢复未选中的视觉状态。
     * Clear button focus and restore its unselected appearance.
     */
    private fun unSelectBtn(textView: AppCompatTextView) {
        textView.isFocusable = false
        textView.isFocusableInTouchMode = false
        textView.clearFocus()
        textView.setTextColor(ContextCompat.getColor(this, R.color.green_70))
        textView.setBackgroundResource(R.drawable.round_unselect_bg)
    }

    /**
     * 记录并聚焦当前按钮，同时更新眼镜菜单的选中样式。
     * Record and focus the current button, and update the glasses menu selection style.
     */
    private fun selectBtn(textView: AppCompatTextView) {
        textView.isClickable = true
        textView.isFocusable = true
        textView.isFocusableInTouchMode = true
        textView.setTextColor(ContextCompat.getColor(this, R.color.green))
        currentSelectId = textView.id
        textView.requestFocus()
        textView.setTextColor(ContextCompat.getColor(this, R.color.green))
        textView.setBackgroundResource(R.drawable.round_select_bg)
        textView.post {
            textView.requestRectangleOnScreen(Rect(0, 0, textView.width, textView.height), true)
        }
    }

    /**
     * 消息或文件发送请求提交到服务端后的结果回调。
     * Result callback after a message or file transfer request is submitted to the server.
     */
    private val mResultCallback = object : IResultCallback.Stub() {
        override fun onSuccess(result: Boolean) {
            Log.d(TAG, "mResultCallback onSuccess() result: $result")
        }

        override fun onFailed(code: Int, errormsg: String?) {
            Log.d(TAG, "mResultCallback onFailed(), code: $code, errormsg: $errormsg")
        }
    }

    private val logBuilder = StringBuilder(4000)

    /**
     * 将最新日志插入顶部，并同步显示到页面日志区域。
     * Insert the latest log at the top and update the log area on the page.
     */
    private fun log(msg: String) {
        if (logBuilder.length > 4000) {
            logBuilder.delete(0, logBuilder.length)
        }
        logBuilder.insert(0, "$msg\n")
        lifecycleScope.launch {
            binding.tvLog.text = logBuilder.toString()
        }
        Log.d(TAG, msg)
    }

    /**
     * 将 Assets 中的演示文件复制到公共 Download 目录，供文件发送接口读取。
     * Copy demo assets to the public Download directory so the file transfer API can read them.
     */
    fun copyFileFromAssetsToExternalStorage(context: Context, fileName: String) {
        var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        try {
            inputStream = context.assets.open(fileName)
            val outputFile = File(sdDownload, fileName)
            outputStream = FileOutputStream(outputFile)
            inputStream.copyTo(outputStream)
            outputStream.flush()
            // 文件写入成功
            // The file was written successfully.
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }


    override fun onDestroy() {
        stopAudioStreamLocally(notifyPhone = true)
        cancelOnlineAsrTimeout()
        super.onDestroy()
        // 页面退出时解除监听并停止仍在运行的语音任务，避免回调持有 Activity。
        // Unregister listeners and stop ongoing speech tasks when leaving the page to avoid callbacks retaining the Activity.
        lifecycleScope.cancel()
        GlassSdk.getGlassMessageService()?.removeMessageListener(mMessageListener)
        mFileOperator?.removeFileReceiveListener(bleFileReceiveListener)
        mBTFileOperator?.removeFileReceiveListener(bleFileReceiveListener)
        stopOnlineAsr()
        cancelOnlineTts()
        speechHandler.removeCallbacksAndMessages(null)
        if (::huoVoiceAction.isInitialized) {
            GlassSdk.getGlassOfflineCmdService()?.remove(huoVoiceAction)
        }
    }

    private fun stopAudioStreamLocally(notifyPhone: Boolean) {
        if (!audioStreamSession.markStopped()) {
            Log.d(TAG, "stopAudioStreamLocally: audio stream is not active")
            return
        }
        val messageService = GlassSdk.getGlassMessageService()
        messageService?.stopAudioStreamData()
        if (notifyPhone) {
            messageService?.sendTextMessageByClassicBT(
                AudioStreamControl.encode(gson, AudioStreamControl.Action.STOP)
            )
        }
        log(demoString(R.string.demo_glasses_stopped_sending_the_audio_stream))
    }

    private fun startAudioStreamRemotely() {
        val messageService = GlassSdk.getGlassMessageService() ?: run {
            log(demoString(R.string.demo_message_service_is_not_initialized_cannot_request_an))
            return
        }
        if (!audioStreamSession.markStarted()) {
            log(demoString(R.string.demo_audio_stream_already_requested))
            return
        }
        messageService.sendTextMessageByClassicBT(
            AudioStreamControl.encode(gson, AudioStreamControl.Action.START)
        )
        log(demoString(R.string.demo_requested_phone_to_start_receiving_audio))
    }


}
