package com.rokid.glass

import com.rokid.glass.utils.demoString

import android.annotation.SuppressLint
import android.graphics.Rect
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rokid.glass.base.BaseGlassActivity
import com.rokid.glass.base.GlassKeyEvent
import com.rokid.glass.media.WorkAudioEncoder
import com.rokid.glass.utils.TimeUtils
import com.rokid.glesse.R
import com.rokid.glesse.databinding.ActivityTestMediaBinding
import com.rokid.security.glass3.open.sdk.GlassSdk
import com.rokid.security.glass3.open.sdk.uitls.log.L
import com.rokid.security.glass3.sdk.base.data.media.PhotoResolution
import com.rokid.security.glass3.sdk.base.data.media.PreviewResolution
import com.rokid.security.glass3.sdk.base.data.media.RecordConfig
import com.rokid.security.system.server.aichat.listener.AiChatListener
import com.rokid.security.system.server.media.callback.AudioCallback
import com.rokid.security.system.server.media.callback.PhotoFileCallback
import com.rokid.security.system.server.media.callback.VideoCallback
import com.rokid.security.system.server.media.listener.IMediaStateLister
import com.rokid.security.system.server.message.callback.IResultCallback
import kotlinx.coroutines.launch
import java.io.File

/**
 * sdk 拍照 录像 录制音频文件 发送视频流等
 * SDK examples for taking photos, recording video/audio files, sending video streams, and related operations.
 */
class SdkMediaActivity : BaseGlassActivity() {

    private lateinit var binding: ActivityTestMediaBinding
    private var TAG = "TestMediaActivity"
    private var zoom = 1
    private var selectBtnStatus = 0
    private var audioEncoder: WorkAudioEncoder? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTestMediaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        GlassSdk.getGlassMediaService()?.addPhotoCallback(mPhotoFileCallback)
        GlassSdk.getGlassMediaService()?.setMediaStateLister(mICameraStateLister)
        unAllSelectState(binding.clMedia)
        selectBtn(binding.btPhoto480P)
        initView()
    }

    @SuppressLint("SetTextI18n")
    private fun initView() {
        binding.btPhoto480P.setOnClickListener {
            log(demoString(R.string.demo_taking_a_480p_photo))
            val fileName = "test_${System.currentTimeMillis()}.png"
            val publicPicturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val file = File(publicPicturesDir, fileName)
            GlassSdk.getGlassMediaService()?.takePhoto(PhotoResolution.RESOLUTION_480P, file.absolutePath)

            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
        }

        binding.btPhoto720P.setOnClickListener {
            log(demoString(R.string.demo_taking_a_720p_photo))
            // 720P拍的照片是横屏
            // Photos taken at 720P are landscape.
            val fileName = "test_${System.currentTimeMillis()}.png"
            val publicPicturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val file = File(publicPicturesDir, fileName)
            GlassSdk.getGlassMediaService()?.takePhoto(PhotoResolution.RESOLUTION_720P, file.absolutePath)

            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
        }

        binding.btPhoto1080P.setOnClickListener {
            log(demoString(R.string.demo_taking_a_1080p_photo))
            // 1080P拍的照片是竖屏
            // Photos taken at 1080P are portrait.
            val fileName = "test_${System.currentTimeMillis()}.png"
            val publicPicturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val file = File(publicPicturesDir, fileName)
            GlassSdk.getGlassMediaService()?.takePhoto(PhotoResolution.RESOLUTION_1080P, file.absolutePath)

            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
        }

        binding.btPhoto1080PLand.setOnClickListener {
            log(demoString(R.string.demo_taking_a_landscape_1080p_photo))
            // 1080P拍的照片是竖屏
            // Photos taken at 1080P are portrait.
            val fileName = "test_${System.currentTimeMillis()}.png"
            val publicPicturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val file = File(publicPicturesDir, fileName)
            GlassSdk.getGlassMediaService()?.takePhoto(PhotoResolution.RESOLUTION_1080P_Land, file.absolutePath)

            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
        }

        binding.btPhoto4K.setOnClickListener {
            log(demoString(R.string.demo_taking_a_4k_photo))
            // 4K 拍照为横屏，分辨率：4032 × 3024
            // 4K photos are landscape, with a resolution of 4032 x 3024.
            val fileName = "test_${System.currentTimeMillis()}.png"
            val publicPicturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val file = File(publicPicturesDir, fileName)
            GlassSdk.getGlassMediaService()?.takePhoto(PhotoResolution.RESOLUTION_4K, file.absolutePath)
            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
        }

        binding.btStartRecord720P.setOnClickListener {
            log(demoString(R.string.demo_starting_720p_video_recording))
            // 720P 录的视频是横屏,分辨率：1280*720
            // Video recorded at 720P is landscape, with a resolution of 1280 x 720.
            // 取值为1则每间隔 1分钟自动生成新文件，后续音视频数据将写入新文件。
            // A value of 1 creates a new file every minute; subsequent audio/video data is written to the new file.
            val min = 1
            val enableAudio = true
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/video"
            Log.d(TAG, "onCreate: path = $path")
            val recordConfig = RecordConfig(
                path,
                min,
                enableAudio,
                width = PreviewResolution.ResolutionInfo_720P.width,
                height = PreviewResolution.ResolutionInfo_720P.height,
                isArRMixEnabled = true
            )
            GlassSdk.getGlassMediaService()?.startRecord(videoCallback, recordConfig)
            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
        }

        binding.btStartRecord1080P.setOnClickListener {
            // 取值为1则每间隔 1分钟自动生成新文件，后续音视频数据将写入新文件。
            // A value of 1 creates a new file every minute; subsequent audio/video data is written to the new file.
            // 1080P 录的视频是竖屏,分辨率：1080 * 1920
            // Video recorded at 1080P is portrait, with a resolution of 1080 x 1920.
            val min = 1
            val enableAudio = true
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/video"
            Log.d(TAG, "onCreate: path = $path")
            val recordConfig = RecordConfig(
                path,
                min,
                enableAudio,
                width = PreviewResolution.ResolutionInfo_1080P.width,
                height = PreviewResolution.ResolutionInfo_1080P.height,
                fps = 20
            )
            // 1080P 目前拍的视频是竖屏
            // Currently, video captured at 1080P is portrait.
            GlassSdk.getGlassMediaService()?.startRecord(videoCallback, recordConfig)
            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
            log(demoString(R.string.demo_starting_1080p_video_recording))
        }

        binding.btStartRecord1080PLand.setOnClickListener {
            // 取值为1则每间隔 1分钟自动生成新文件，后续音视频数据将写入新文件。
            // A value of 1 creates a new file every minute; subsequent audio/video data is written to the new file.
            // 1080P 录的视频是竖屏,分辨率：1080 * 1920
            // Video recorded at 1080P is portrait, with a resolution of 1080 x 1920.
            val min = 1
            val enableAudio = true
            val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath + "/video"
            Log.d(TAG, "onCreate: path = $path")
            val recordConfig = RecordConfig(
                path,
                min,
                enableAudio,
                width = PreviewResolution.ResolutionInfo_1080P_Land.width,
                height = PreviewResolution.ResolutionInfo_1080P_Land.height,
                fps = 20
            )
            // 1080P 目前拍的视频是竖屏
            // Currently, video captured at 1080P is portrait.
            GlassSdk.getGlassMediaService()?.startRecord(videoCallback, recordConfig)
            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
            log(demoString(R.string.demo_starting_landscape_1080p_video_recording))
        }

        binding.btStopRecord.setOnClickListener {
            log(demoString(R.string.demo_stopping_video_recording))
            GlassSdk.getGlassMediaService()?.stopRecord()
            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
        }

        binding.btSendVideo.setOnClickListener {
            log(demoString(R.string.demo_starting_video_transmission))
            GlassSdk.getGlassMessageService()?.sendVideoStreamDataV2(object : IResultCallback.Default() {
                override fun onSuccess(result: Boolean) {
                    super.onSuccess(result)
                    log(demoString(R.string.demo_sendvideostreamdata_onsuccess_status_10))
                }

                override fun onFailed(code: Int, errormsg: String?) {
                    super.onFailed(code, errormsg)
                    log(demoString(R.string.demo_sendvideostreamdata_onfailed_code_errormsg_status_11, code, errormsg))
                }
            })
            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
        }

        binding.btStopSendVideo.setOnClickListener {
            log(demoString(R.string.demo_stopping_video_transmission))
            GlassSdk.getGlassMessageService()?.stopVideoStreamData()
            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
        }
        binding.btnSetZoom.setOnClickListener {
            binding.btnSetZoom.text = demoString(R.string.demo_zoom_status_12, zoom)
            // sdk的数字变焦方法，只有视频录像才会起作用，照片是不起作用的
            // The SDK's digital zoom only affects video recording, not photos.
            GlassSdk.getGlassMediaService()?.zoomCamera(zoom)
            zoom++
            if (zoom > 3) {
                zoom = 1
            }
            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
            log(demoString(R.string.demo_camera_zoom, GlassSdk.getGlassMediaService()?.zoomLevel))
        }
        binding.btnRecordAudio.setOnClickListener {
            log(demoString(R.string.demo_starting_audio_recording))
            stopAudioWrite()
            val fileName = TimeUtils.timestampToDateTime(System.currentTimeMillis(), "yyyy-MM-dd-HH-mm-ss") + ".aac"
            val picDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val path = File(picDir, fileName).absolutePath
            audioEncoder = WorkAudioEncoder()
            audioEncoder?.start(path)
            GlassSdk.getGlassMediaService()?.startAudioRecord(mAudioCallback)
            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
        }
        binding.btnStopRecordAudio.setOnClickListener {
            log(demoString(R.string.demo_stopping_audio_recording))
            GlassSdk.getGlassMediaService()?.stopAudioRecord(mAudioCallback)
            stopAudioWrite()
            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
        }
        binding.btnAiChat.setOnClickListener {
            log(demoString(R.string.demo_starting_ai_chat))
            GlassSdk.getGlassAiChatService()?.startAiChat(false)
            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
            GlassSdk.getGlassAiChatService()?.toAiChat(demoString(R.string.demo_will_it_rain_in_hangzhou_tomorrow), mAIChaLister)
        }
        binding.btnStopAiChat.setOnClickListener {
            log(demoString(R.string.demo_stopping_ai_chat))
            GlassSdk.getGlassAiChatService()?.endAiChat()
            unAllSelectState(binding.clMedia)
            selectBtn(it as AppCompatButton)
        }
    }

    private fun stopAudioWrite() {
        audioEncoder?.stop()
        audioEncoder = null
    }

    private fun unSelectBtn(textView: AppCompatButton) {
        textView.clearFocus()
        textView.isSelected = false
        textView.isClickable = false
        textView.isFocusable = false
        textView.setTextColor(ContextCompat.getColor(this, R.color.green_70))
        textView.setBackgroundResource(R.drawable.round_unselect_bg)
    }

    private fun selectBtn(textView: AppCompatButton) {
        textView.setTextColor(ContextCompat.getColor(this, R.color.green))
        textView.setBackgroundResource(R.drawable.round_select_bg)
        textView.isSelected = true
        textView.isClickable = true
        textView.isFocusable = true
        textView.isFocusableInTouchMode = true
        textView.requestFocus()
        textView.post {
            textView.requestRectangleOnScreen(Rect(0, 0, textView.width, textView.height), true)
        }
    }

    override fun onGlassKeyEvent(keyEvent: Int): Boolean {
        when (keyEvent) {
            GlassKeyEvent.KEYCODE_FRONT -> {
                selectBtnStatus++
                selectBtnStatus %= 16
                when (selectBtnStatus) {
                    1 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btPhoto720P)
                    }

                    2 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btPhoto1080P)
                    }

                    3 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btPhoto1080PLand)
                    }

                    4 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btPhoto4K)
                    }

                    5 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btStartRecord720P)
                    }

                    6 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btStartRecord1080P)
                    }

                    7 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btStartRecord1080PLand)
                    }

                    8 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btStopRecord)
                    }

                    9 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btSendVideo)
                    }

                    10 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btStopSendVideo)
                    }

                    11 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btnSetZoom)
                    }

                    12 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btnRecordAudio)
                    }

                    13 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btnStopRecordAudio)
                    }

                    14 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btnAiChat)
                    }

                    15 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btnStopAiChat)
                    }

                    0 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btPhoto480P)
                    }
                }
            }

            GlassKeyEvent.KEYCODE_BEHIND -> {
                selectBtnStatus--
                if (selectBtnStatus < 0) {
                    selectBtnStatus = 15
                }
                selectBtnStatus %= 16
                when (selectBtnStatus) {
                    1 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btPhoto720P)
                    }

                    2 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btPhoto1080P)
                    }

                    3 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btPhoto1080PLand)
                    }

                    4 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btPhoto4K)
                    }

                    5 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btStartRecord720P)
                    }

                    6 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btStartRecord1080P)
                    }

                    7 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btStartRecord1080PLand)
                    }

                    8 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btStopRecord)
                    }

                    9 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btSendVideo)
                    }

                    10 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btStopSendVideo)
                    }

                    11 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btnSetZoom)
                    }

                    12 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btnRecordAudio)
                    }

                    13 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btnStopRecordAudio)
                    }

                    14 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btnAiChat)
                    }

                    15 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btnStopAiChat)
                    }

                    0 -> {
                        unAllSelectState(binding.clMedia)
                        selectBtn(binding.btPhoto480P)
                    }
                }
            }

            GlassKeyEvent.KEYCODE_CLICK -> {
                findSelectedButton(binding.clMedia)?.performClick()
            }
        }
        return super.onGlassKeyEvent(keyEvent)
    }

    private fun findSelectedButton(viewGroup: ViewGroup): AppCompatButton? {
        for (i in 0 until viewGroup.childCount) {
            val child = viewGroup.getChildAt(i)
            if (child is AppCompatButton && child.isSelected) return child
            if (child is ViewGroup) {
                findSelectedButton(child)?.let { return it }
            }
        }
        return null
    }

    fun unAllSelectState(viewGroup: ViewGroup) {
        for (i in 0 until viewGroup.childCount) {
            if (viewGroup.getChildAt(i) is ViewGroup) {
                unAllSelectState(viewGroup.getChildAt(i) as ViewGroup)
            } else if (viewGroup.getChildAt(i) is AppCompatButton) {
                unSelectBtn(viewGroup.getChildAt(i) as AppCompatButton)
            }
        }
    }

    private val mAudioCallback = object : AudioCallback.Stub() {
        override fun onAudioStream(buffer: ByteArray?, bufferLen: Int) {
            if (buffer != null && bufferLen > 0) {
                audioEncoder?.encode(buffer, bufferLen)
            }
        }

        override fun getCallbackId(): String {
            return "10001"
        }
    }

    private val mPhotoFileCallback = object : PhotoFileCallback.Stub() {
        override fun onTakePhoto(path: String) {
            log(demoString(R.string.demo_ontakephoto_path_status_13, path))
        }

        override fun getCallbackId(): String {
            return "10002"
        }

        override fun onTakePhotoV2(path: String, width: Int, height: Int) {
            log(demoString(R.string.demo_ontakephoto_width_height_path_status_14, width, height, path))
        }
    }

    private val mICameraStateLister = object : IMediaStateLister.Stub() {
        override fun onCameraResolutionChange(width: Int, height: Int) {
            log(demoString(R.string.demo_oncameraresolutionchange_width_height_status_15, width, height))
        }

        override fun onCameraError(code: Int, errorMsg: String) {
            log(demoString(R.string.demo_oncameraerror_code_errormsg_status_16, code, errorMsg))
        }
    }

    private val mAIChaLister = object : AiChatListener.Stub() {
        override fun onContinuousModeUpdate(continuousMode: Boolean, timeout: Long, keepSessionActive: Boolean) {
            log(demoString(R.string.demo_oncontinuousmodeupdate_continuousmode_timeout_keepsessionactive_status_17, continuousMode, timeout, keepSessionActive))
        }

        /**
         * ai回答的答案
         * Answer returned by the AI.
         * @param answer 答案，流式的
         * The streamed answer.
         * @param isFinish 答案的终止符号
         * Whether the answer has finished.
         */
        override fun onAiChatAnswer(answer: String?, isFinish: Boolean, contentType: String?, sessionId: String?) {
            log(demoString(R.string.demo_onaichatanswer_answer_isfinish_contenttype_sessionid_status_18, answer, isFinish, contentType, sessionId))
        }

        /**
         * ai问题的错误回调
         * Error callback for an AI question.
         * @param code 错误码
         * The error code.
         * @param message 错误信息
         * The error message.
         */
        override fun onError(code: Int, message: String) {
            log(demoString(R.string.demo_aichat_onerror_code_message_status_19, code, message))
        }

        override fun onAiTakePhoto(filePath: String) {
            log(demoString(R.string.demo_onaitakephoto_filepath_status_20, filePath))
        }
    }

    private val videoCallback = object : VideoCallback.Stub() {
        override fun onError() {
            log(demoString(R.string.demo_onerror_status_21))
        }

        override fun onFinish() {
            log(demoString(R.string.demo_onfinish_status_22))
        }

        override fun onNewFile(startTime: Long, endTime: Long, path: String, isLast: Boolean) {
            log(demoString(R.string.demo_onnewfile_status_23, path))
        }

        override fun onErrorWithDetail(code: Int, errorMsg: String) {
            log(demoString(R.string.demo_onerror_code_errormsg_status_24, code, errorMsg))
        }

        override fun onStart() {
            log(demoString(R.string.demo_onstart_status_25))
        }
    }

    private val logBuilder = StringBuilder()
    private fun log(msg: String) {
        if (logBuilder.length > 5000) {
            logBuilder.clear()
        }
        logBuilder.insert(0, "$msg\n")
        lifecycleScope.launch {
            binding.tvLog.text = logBuilder.toString()
        }
        Log.d(TAG,"-----------msg=${msg}")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "onDestroy: ")
        GlassSdk.getGlassMediaService()?.removePhotoCallback(mPhotoFileCallback)
        GlassSdk.getGlassMediaService()?.removeMediaStateLister(mICameraStateLister)
        GlassSdk.getGlassMediaService()?.stopAudioRecord(mAudioCallback)
        GlassSdk.getGlassMediaService()?.stopRecord()
        stopAudioWrite()
    }

}
