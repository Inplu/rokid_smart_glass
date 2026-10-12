package com.rokid.glass

import com.rokid.glass.utils.demoString
import com.rokid.glesse.R

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.rokid.glass.base.BaseActivity
import com.rokid.glass.utils.FileSizeUtil
import com.rokid.glass.utils.TimeUtils
import com.rokid.glesse.databinding.ActivityFaceTrackBinding
import com.rokid.security.glass3.open.sdk.GlassSdk
import com.rokid.security.glass3.sdk.base.data.recog.offline.bean.FaceModel
import com.rokid.security.glass3.sdk.base.data.recog.offline.bean.LPRModel
import com.rokid.security.sdk.base.common.out.AIRecgMode.Companion.MODE_FACE
import com.rokid.security.system.server.media.callback.VideoCallback
import com.rokid.security.system.server.recog.online.listener.IGlassDetectionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GlassFaceTrackActivity : BaseActivity() {
    private val TAG = "GlassTrackActivity"
    private lateinit var binding: ActivityFaceTrackBinding
    private var lastTrackId: Long = -1L
    private var oldBitmap: Bitmap? = null
    private var lastTime = 0L
    private val logBuilder = StringBuilder()

    /**
     * 离线和在线人脸检测功能
     * Offline and online face detection features.
     */
    private val mAbsGlassOnlineRecService by lazy {
        GlassSdk.getGlassOnlineRecService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFaceTrackBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
    }

    private fun initView() {
        // 设置人脸检测监听
        // Set the face detection listener.
        mAbsGlassOnlineRecService?.setGlassOnlineRecListener(mIGlassDetectionListener)
        // 开始人脸检测
        // Start face detection.
        mAbsGlassOnlineRecService?.startDetection(MODE_FACE)
        lastTime = System.currentTimeMillis()
    }

    /**
     * 视频录制回调
     * Video recording callback.
     */
    private val videoCallback = object : VideoCallback.Stub() {
        override fun onError() {
            Log.d(TAG, "onError: ")
        }

        override fun onFinish() {
            Log.d(TAG, "onFinish: ")
        }

        override fun onNewFile(startTime: Long, endTime: Long, path: String, isLast: Boolean) {
            Log.d(TAG, "onNewFile: $path")
        }

        override fun onErrorWithDetail(code: Int, errorMsg: String) {
            Log.d(TAG, "onError: code = $code, errorMsg = $errorMsg")
        }

        override fun onStart() {
            Log.d(TAG, "onStart: ")
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止人脸检测
        // Stop face detection.
        mAbsGlassOnlineRecService?.stopDetection()
        // 移除检测监听
        // Remove the detection listener.
        mAbsGlassOnlineRecService?.removeGlassOnlineRecListener(mIGlassDetectionListener)
        if (oldBitmap?.isRecycled == false) {
            oldBitmap?.recycle()
            oldBitmap = null
        }
    }

    override fun onGlassKeyEvent(keyEvent: Int): Boolean {
        super.onGlassKeyEvent(keyEvent)
        if (keyEvent == com.rokid.glass.base.GlassKeyEvent.KEYCODE_FRONT) {
        } else if (keyEvent == com.rokid.glass.base.GlassKeyEvent.KEYCODE_BEHIND) {
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * 人脸检测监听
     * Face detection listener.
     */
    private val mIGlassDetectionListener = object : IGlassDetectionListener.Stub() {

        /**
         * 检测模式发送改变的回调
         * Callback for detection mode changes.
         * @param mode 检测模式
         * The detection mode.
         */
        override fun onModeChange(mode: Int) {
            Log.e(TAG, "onModeChange: $mode")
        }

        /**
         * 人脸检测的回调(可用于更新人脸追踪框)
         * Face detection callback, which can be used to update face tracking boxes.
         * @param faceModels 人脸模型列表
         * The list of face models.
         */
        override fun onFaceTrack(faceModels: List<FaceModel>) {
        }

        /**
         * 车牌识别的回调
         * License plate recognition callback.
         * @param lprModel 车牌模型
         * The license plate model.
         */
        override fun onLPRTrack(lprModel: LPRModel) {
        }

        /**
         * 筛选最优的人脸图像 (可用于在线人脸识别接口的调用)
         * Select the best face image, which can be used with the online face recognition API.
         * @param processedFaceModels 处理后的人脸模型列表
         * The list of processed face models.
         */
        @SuppressLint("SetTextI18n")
        override fun onProcessedFaceModels(processedFaceModels: List<FaceModel>) {
            if (processedFaceModels.isEmpty()) {
                log(demoString(R.string.demo_no_face_data))
                return
            }
            val faceModel = processedFaceModels.maxByOrNull { it.rect.width() * it.rect.height() }
            if (faceModel == null) {
                log(demoString(R.string.demo_no_face_data_meets_the_requirements))
                return
            }
            var duration = System.currentTimeMillis() - lastTime
            // 图像质量低于35人脸检测不到
            // Faces cannot be detected when the image quality is below 35.
            if (faceModel.iqaScore < 40) {
                lastTime = System.currentTimeMillis()
                val faeInfo =
                    StringBuilder(
                        demoString(R.string.demo_duration_low_image_quality_face_confidence_tracking_id, TimeUtils.formatDuration(duration), "%.1f".format(faceModel.iqaScore),
                            "%.1f".format(faceModel.faceScore),
                            faceModel.trackId)
                    )
                log(faeInfo.toString())
                return
            }
            val faeInfo =
                StringBuilder(
                    demoString(R.string.demo_duration_image_quality_face_confidence_tracking_id, TimeUtils.formatDuration(duration), "%.1f".format(faceModel.iqaScore),
                        "%.1f".format(faceModel.faceScore),
                        faceModel.trackId)
                )
            if (faceModel.trackId == lastTrackId) {
                lastTime = System.currentTimeMillis()
                faeInfo.append(demoString(R.string.demo_same_face_image))
                log(faeInfo.toString())
                return
            }
            // 先更新人脸抓拍图
            // Update the captured face image first.
            val smallBitmap = mAbsGlassOnlineRecService?.getFaceSamllBitmap(faceModel.trackId)
            duration = System.currentTimeMillis() - lastTime
            lastTime = System.currentTimeMillis()
            if (smallBitmap == null) {
                log(demoString(R.string.demo_duration_no_face_image_received, TimeUtils.formatDuration(duration)))
                return
            }

            if (smallBitmap.isRecycled) {
                return
            }
            // 回收上一张图片
            // Recycle the previous image.
            if (oldBitmap?.isRecycled == false) {
                Log.d(TAG, "回收上一张人脸图片: ${getBitmapSizeInfo(oldBitmap)}")
                oldBitmap?.recycle()
                oldBitmap = null
            }
            oldBitmap = smallBitmap
            val oldBitmapSizeInfo = getBitmapSizeInfo(smallBitmap)
            lifecycleScope.launch(Dispatchers.Main) {
                binding.ivCapture.setImageBitmap(smallBitmap)
                // float 类型保留1位
                // Keep one decimal place for the floating-point value.
                var result =
                    demoString(R.string.demo_duration_quality_tracking_id_image_size, TimeUtils.formatDuration(duration),
                        "%.1f".format(faceModel.iqaScore),
                        faceModel.trackId, oldBitmapSizeInfo)
                log(result)
            }
        }
    }

    private fun getBitmapSizeInfo(bitmap: Bitmap?): String {
        if (bitmap == null || bitmap.isRecycled) {
            return "0 B"
        }
        val sizeText = FileSizeUtil.formatFileSize(bitmap.allocationByteCount.toDouble())
        return "${bitmap.width}x${bitmap.height}, $sizeText, ${bitmap.config}"
    }

    private fun log(msg: String) {
        if (logBuilder.length > 5000) {
            logBuilder.clear()
        }
        logBuilder.insert(0, "$msg\n")
        lifecycleScope.launch(Dispatchers.Main) {
            binding.tvLog.text = logBuilder.toString()
        }
        Log.d(TAG, "----msg=${msg}")
    }

}
