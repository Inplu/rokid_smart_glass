package com.rokid.glass

import com.rokid.glass.utils.demoString
import com.rokid.glesse.R

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.rokid.glass.base.BaseActivity
import com.rokid.glass.data.GlobalData
import com.rokid.glesse.databinding.ActivityTrackBinding
import com.rokid.security.glass3.open.sdk.GlassSdk
import com.rokid.security.glass3.sdk.base.data.media.PreviewResolution
import com.rokid.security.glass3.sdk.base.data.media.RecordConfig
import com.rokid.security.glass3.sdk.base.data.recog.offline.bean.FaceModel
import com.rokid.security.glass3.sdk.base.data.recog.offline.bean.LPRModel
import com.rokid.security.sdk.base.common.out.AIRecgMode.Companion.MODE_LPR
import com.rokid.security.system.server.media.callback.VideoCallback
import com.rokid.security.system.server.recog.online.listener.IGlassDetectionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GlassLprTrackActivity : BaseActivity() {
    private val TAG = "GlassLprTrackActivity"
    private lateinit var binding: ActivityTrackBinding

    private val mAbsGlassTrackService by lazy {
        GlassSdk.getGlassOnlineRecService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrackBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 添加车牌识别回调
        // Add the license plate recognition callback.
        mAbsGlassTrackService?.setGlassOnlineRecListener(mTrackListener)
        // 离线车牌识别模式
        // Offline license plate recognition mode.
        mAbsGlassTrackService?.startDetection(MODE_LPR)

    }

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

    /**
     *  车牌识别回调
     * License plate recognition callback.
     */
    private val mTrackListener = object : IGlassDetectionListener.Stub() {
        /**
         * 检测模式发送改变的回调
         * Callback for detection mode changes.
         * @param mode
         */
        override fun onModeChange(mode: Int) {
            // 还有在扫描的情况下更改状态MODE_NONE，onModeChange这个方法也没有执行
            // Changing the state to MODE_NONE during scanning also failed to invoke onModeChange.
        }

        /**
         * 人脸检测的回调(可用与更新人脸追踪框)
         * Face detection callback, which can be used to update face tracking boxes.
         * @param faceModels
         */
        override fun onFaceTrack(faceModels: List<FaceModel>) {
        }

        /**
         * 车牌识别的回调
         * License plate recognition callback.
         * @param lprModel
         */
        @SuppressLint("DefaultLocale")
        override fun onLPRTrack(lprModel: LPRModel) {
            lifecycleScope.launch(Dispatchers.Main) {
                Log.e(TAG, "车牌号:${lprModel.plateNo}")
                val msg = String.format(demoString(R.string.demo_plate_color_plate_number_score), lprModel.color, lprModel.plateNo, lprModel.score)
                log(msg)
                if (GlobalData.btConnectState.value) {
                    GlassSdk.getGlassMessageService()?.sendTextMessageByClassicBT(msg)
                }
            }
        }

        /**
         * 筛选最优的人脸图像 (可用于在线人脸识别接口的调用)
         * Select the best face image, which can be used with the online face recognition API.
         */
        override fun onProcessedFaceModels(processedFaceModels: List<FaceModel>) {
        }
    }

    private val logBuilder = StringBuilder()
    private fun log(msg: String) {
        if (logBuilder.length > 5000) {
            logBuilder.clear()
        }
        logBuilder.insert(0, "$msg\n")
        binding.tvLog.text = logBuilder.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 停止车牌识别
        // Stop license plate recognition.
        mAbsGlassTrackService?.stopDetection()
        // 移除车牌识别回调
        // Remove the license plate recognition callback.
        mAbsGlassTrackService?.removeGlassOnlineRecListener(mTrackListener)
    }

    override fun onResume() {
        super.onResume()
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onPause() {
        super.onPause()
        window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

}
