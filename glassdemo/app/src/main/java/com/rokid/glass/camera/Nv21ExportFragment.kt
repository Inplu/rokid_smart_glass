package com.rokid.glass.camera

import com.rokid.glass.utils.demoString

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.graphics.Rect
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.rokid.glass.base.GlassKeyEvent
import com.rokid.glass.view.glsurface.view.BackgroundGLSurfaceView
import com.rokid.glesse.R
import com.rokid.security.glass3.open.sdk.GlassSdk
import com.rokid.security.glass3.open.sdk.camera.CameraShareHelper
import com.rokid.security.glass3.sdk.base.data.media.CameraShareConfig

/**
 * NV21 导出模式 Fragment
 * Fragment for NV21 export mode.
 * 显示纯相机 NV21 数据（不含屏幕叠加）
 * Display raw camera NV21 data without screen overlays.
 * 支持用户自定义分辨率、FPS、Zoom、防抖等参数
 * Allow custom resolution, FPS, zoom, stabilization, and other parameters.
 */
class Nv21ExportFragment : Fragment(), CameraShareGestureHandler {

    companion object {
        private const val TAG = "Nv21ExportFragment"

        fun newInstance(): Nv21ExportFragment {
            return Nv21ExportFragment()
        }

        // 支持的 FPS 选项
        // Supported FPS options.
        private val FPS_OPTIONS = listOf(15, 20, 24, 30)

        /**
         * 获取 Camera 支持的分辨率列表（对调宽高以适配竖屏显示）
         * Get camera-supported resolutions, swapping width and height for portrait display.
         *
         * 通过 AIDL 接口从 system-server 动态获取 Camera API 支持的分辨率列表
         * Dynamically obtain the resolutions supported by the Camera API from system-server through AIDL.
         * 过滤掉高于 2268x3024 的分辨率
         * Filter out resolutions above 2268 x 3024.
         *
         * @return List<Triple<displayText, width, height>>
         */
        fun getSupportedResolutions(): List<Pair<String, Triple<Int, Int, Boolean>>> {
            return try {
                Log.d(TAG, "getSupportedResolutions: calling CameraShareHelper.getSupportedPreviewSizes()...")
                val cameraSizes = CameraShareHelper().getSupportedPreviewSizes()
                if (cameraSizes.isNotEmpty()) {
                    Log.d(TAG, "getSupportedResolutions: successfully got ${cameraSizes.size} sizes from AIDL")

                    // 过滤掉高于 2268x3024 的分辨率
                    // Filter out resolutions above 2268 x 3024.
                    val filteredSizes = cameraSizes.filter { size ->
                        val maxDimension = maxOf(size.first, size.second)
                        val minDimension = minOf(size.first, size.second)
                        // 过滤条件：最大维度 <= 3024 且 最小维度 <= 2268
                        // Filter condition: the larger dimension is at most 3024 and the smaller dimension is at most 2268.
                        maxDimension <= 3024 && minDimension <= 2268
                    }

                    Log.d(TAG, "getSupportedResolutions: filtered from ${cameraSizes.size} to ${filteredSizes.size} sizes (max 2268x3024)")

                    if (filteredSizes.isNotEmpty()) {
                        // 按分辨率从高到低排序
                        // Sort resolutions from highest to lowest.
                        val result = filteredSizes.map { size ->
                            val displayText = demoString(R.string.demo_x, size.first, size.second, if (size.third) demoString(R.string.demo_portrait) else demoString(R.string.demo_landscape))
                            Log.d(TAG, "getSupportedResolutions: ${displayText}")
                            displayText to size
                        }.sortedByDescending { it.second.first * it.second.second }
                        Log.d(TAG, "getSupportedResolutions: returning ${result.size} resolutions")
                        result
                    } else {
                        Log.w(TAG, "getSupportedResolutions: all sizes filtered out, using fallback")
                        getFallbackResolutions()
                    }
                } else {
                    Log.w(TAG, "getSupportedResolutions: AIDL returned empty list, using fallback")
                    getFallbackResolutions()
                }
            } catch (e: Exception) {
                Log.e(TAG, "getSupportedResolutions: failed to get Camera sizes via AIDL, using fallback: ${e.message}", e)
                getFallbackResolutions()
            }
        }

        /**
         * Fallback 分辨率列表（当 Camera API 不可用时使用）
         * Fallback resolution list for when the Camera API is unavailable.
         * 注意：这里的分辨率是视觉效果分辨率（已对调宽高）
         * Note: these are visual display resolutions, with width and height already swapped.
         * - Camera 原始分辨率是横屏的（width > height），例如 1280x720
         * - The camera's original resolution is landscape (width > height), for example 1280 x 720.
         * - 对调后变成竖屏视觉效果，例如 720x1280
         * - Swapping the dimensions produces a portrait display, for example 720 x 1280.
         */
        private fun getFallbackResolutions(): List<Pair<String, Triple<Int, Int, Boolean>>> {
            return listOf(
                demoString(R.string.demo_640x480_landscape) to Triple(640, 480, false),
                demoString(R.string.demo_480x640_portrait) to Triple(480, 640, true),
                demoString(R.string.demo_1280x720_landscape) to Triple(1280, 720, false),
                demoString(R.string.demo_720x1280_portrait) to Triple(720, 1280, true),
                demoString(R.string.demo_1920x1080_landscape) to Triple(1920, 1080, false),
                demoString(R.string.demo_1080x1920_portrait) to Triple(1080, 1920, true),
            )
        }
    }

    private lateinit var nv21SurfaceView: BackgroundGLSurfaceView
    private lateinit var tvInfo: TextView
    private lateinit var configPanel: View
    private lateinit var btnShowConfig: Button
    private lateinit var btnApply: Button
    private lateinit var btnToggleConfig: Button
    private lateinit var spinnerResolution: Spinner
    private lateinit var spinnerFps: Spinner
    private lateinit var seekbarZoom: SeekBar
    private lateinit var tvZoomValue: TextView
    private lateinit var switchEis: Switch
    private lateinit var configControls: List<View>
    private var selectedConfigIndex = 0

    private val nv21Helper = CameraShareHelper()
    private var configSwitchJob: Job? = null
    @Volatile private var callbackGeneration = 0
    private val previewPreferences get() = requireContext().getSharedPreferences("nv21_preview", 0)
    private var frameCount = 0L
    private var lastFpsTime = 0L
    private var lastRenderedFrameCount = 0L
    private var lastDroppedFrameCount = 0L
    private var currentWidth = 0
    private var currentHeight = 0
    // 实际应用的 FPS
    // The FPS actually applied.
    private var currentFps = 0
    // 实际应用的 EIS 状态
    // The EIS state actually applied.
    private var currentEisEnabled = false
    // 实际应用的 Zoom
    // The zoom actually applied.
    private var currentZoomLevel = 1
    // 用于调试帧间隔
    // Used to debug frame intervals.
    private var lastFrameTimestamp = 0L
    private var frameIntervalSum = 0L
    private var frameIntervalCount = 0

    // 当前配置
    // Current configuration.
    // 默认选择 1920x1080（会在初始化时查找）
    // Select 1920 x 1080 by default; it will be located during initialization.
    private var currentResolutionIndex = 0
    // 默认 15fps
    // Default: 15 fps.
    private var currentFpsIndex = 0
    // 默认 zoom = 1
    // Default: zoom = 1.
    private var currentZoom = 1
    // 默认关闭防抖
    // Disable stabilization by default.
    private var currentEis = false
    private var currentConfigText: String = ""

    // 动态获取的分辨率列表
    // Dynamically obtained resolution list.
    private lateinit var resolutionOptions: List<Pair<String, Triple<Int, Int, Boolean>>>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nv21_export, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 动态获取 Camera 支持的分辨率列表（已过滤高于 2268x3024 的分辨率）
        // Dynamically obtain the resolutions supported by Camera, excluding resolutions above 2268 x 3024.
        resolutionOptions = getSupportedResolutions()

        val preferences = previewPreferences
        val savedWidth = preferences.getInt("width", 1920)
        val savedHeight = preferences.getInt("height", 1080)
        currentResolutionIndex = resolutionOptions.indexOfFirst {
            it.second.first == savedWidth && it.second.second == savedHeight
        }.takeIf { it >= 0 } ?: resolutionOptions.indexOfFirst {
            it.second.first == 1920 && it.second.second == 1080
        }.coerceAtLeast(0)
        currentFpsIndex = FPS_OPTIONS.indexOf(preferences.getInt("fps", 15)).coerceAtLeast(0)
        currentZoom = preferences.getInt("zoom", 1).coerceIn(1, 8)
        currentEis = preferences.getBoolean("eis", false)
        Log.d(TAG, "Restored requested resolution: ${resolutionOptions[currentResolutionIndex].first}")

        nv21SurfaceView = view.findViewById(R.id.nv21_surface_view)
        tvInfo = view.findViewById(R.id.tv_info)
        configPanel = view.findViewById(R.id.config_panel)
        btnShowConfig = view.findViewById(R.id.btn_show_config)
        btnApply = view.findViewById(R.id.btn_apply)
        btnToggleConfig = view.findViewById(R.id.btn_toggle_config)
        spinnerResolution = view.findViewById(R.id.spinner_resolution)
        spinnerFps = view.findViewById(R.id.spinner_fps)
        seekbarZoom = view.findViewById(R.id.seekbar_zoom)
        tvZoomValue = view.findViewById(R.id.tv_zoom_value)
        switchEis = view.findViewById(R.id.switch_eis)
        configControls = listOf(
            view.findViewById<View>(R.id.row_resolution),
            view.findViewById<View>(R.id.row_fps),
            view.findViewById<View>(R.id.row_zoom),
            view.findViewById<View>(R.id.row_eis),
            btnApply,
            btnToggleConfig,
        )
        configControls.forEach { it.isFocusable = false }

        setupConfigUI()
        btnShowConfig.isFocusable = false
        startNv21Export()
    }

    override fun onResume() {
        super.onResume()
        if (::nv21SurfaceView.isInitialized) {
            nv21SurfaceView.onResume()
        }
    }

    override fun onPause() {
        releaseNv21Resources("onPause")
        if (::nv21SurfaceView.isInitialized) {
            nv21SurfaceView.onPause()
        }
        super.onPause()
    }

    override fun onDestroyView() {
        releaseNv21Resources("onDestroyView")
        if (::nv21SurfaceView.isInitialized) {
            nv21SurfaceView.releasePreview()
        }
        super.onDestroyView()
    }

    private fun releaseNv21Resources(reason: String) {
        callbackGeneration++
        configSwitchJob?.cancel()
        configSwitchJob = null
        if (!nv21Helper.isNv21Active()) return
        Log.d(TAG, "$reason: releasing NV21 export")
        try {
            nv21Helper.releaseNv21Export()
            Log.d(TAG, "$reason: NV21 export released")
        } catch (e: Exception) {
            Log.e(TAG, "$reason: failed to release NV21 export: ${e.message}", e)
        }
    }

    /**
     * 设置配置 UI
     * Set up the configuration UI.
     */
    private fun setupConfigUI() {
        // 分辨率 Spinner - 使用动态获取的分辨率列表
        // Resolution Spinner: use the dynamically obtained resolution list.
        val resolutionAdapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_item,
            resolutionOptions.map { it.first }
        )
        resolutionAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        spinnerResolution.adapter = resolutionAdapter
        spinnerResolution.setSelection(currentResolutionIndex.coerceIn(0, resolutionOptions.size - 1))
        spinnerResolution.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentResolutionIndex = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // FPS Spinner
        val fpsAdapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_item,
            FPS_OPTIONS.map { "${it} fps" }
        )
        fpsAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
        spinnerFps.adapter = fpsAdapter
        spinnerFps.setSelection(currentFpsIndex)
        spinnerFps.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentFpsIndex = position
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Zoom SeekBar
        seekbarZoom.progress = currentZoom
        tvZoomValue.text = currentZoom.toString()
        seekbarZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentZoom = progress.coerceIn(1, 8)
                tvZoomValue.text = currentZoom.toString()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // EIS Switch
        switchEis.isChecked = currentEis
        switchEis.setOnCheckedChangeListener { _, isChecked ->
            currentEis = isChecked
        }

        // 显示配置按钮
        // Show-configuration button.
        btnShowConfig.setOnClickListener {
            setConfigPanelVisible(true)
        }

        // 隐藏配置按钮
        // Hide-configuration button.
        btnToggleConfig.setOnClickListener {
            setConfigPanelVisible(false)
        }

        // 应用配置按钮
        // Apply-configuration button.
        btnApply.setOnClickListener {
            applyConfig()
        }
    }

    /**
     * 应用用户配置
     * Apply the user's configuration.
     */
    private fun applyConfig() {
        if (configSwitchJob?.isActive == true) return
        val resolutionOption = resolutionOptions[currentResolutionIndex].second
        val width = resolutionOption.first
        val height = resolutionOption.second
        val isPortrait = resolutionOption.third
        val fps = FPS_OPTIONS[currentFpsIndex]

        Log.d(TAG, "Applying config: ${width}x${height}@${fps}fps, zoom=$currentZoom, EIS=$currentEis, isPortrait=$isPortrait")

        val newConfig = CameraShareConfig(
            previewWidth = width,
            previewHeight = height,
            previewTargetFps = fps,
            enableVideoStabilization = currentEis,
            zoomLevel = currentZoom,
        )

        Log.d(TAG, "applyConfig: ${width}x${height}@${fps}fps, zoom=$currentZoom")
        // Avoid immediate stop/start reusing the old producer on current firmware.
        // Only release our consumer; never force-close other camera consumers.
        callbackGeneration++
        val firstFrame = CompletableDeferred<Unit>()
        configSwitchJob = viewLifecycleOwner.lifecycleScope.launch {
            btnApply.isEnabled = false
            try {
                nv21Helper.releaseNv21Export()
                nv21SurfaceView.releasePreview()
                nv21SurfaceView.requestRender()
                currentWidth = 0
                currentHeight = 0
                frameCount = 0
                lastFpsTime = 0
                lastRenderedFrameCount = 0
                lastDroppedFrameCount = 0
                lastFrameTimestamp = 0
                frameIntervalSum = 0
                frameIntervalCount = 0
                Log.d(TAG, "Config switch: released own consumer; waiting for idle cleanup")
                delay(1500)
                nv21Helper.initNv21ExportWithConfig(
                    enableMix = false,
                    config = newConfig,
                    callback = createNv21Callback(isPortrait, newConfig, firstFrame),
                )
                withTimeout(10_000) { firstFrame.await() }
                previewPreferences.edit()
                    .putInt("width", width).putInt("height", height).putInt("fps", fps)
                    .putInt("zoom", newConfig.zoomLevel).putBoolean("eis", newConfig.enableVideoStabilization)
                    .apply()
                currentConfigText = demoString(R.string.demo_requested_x_fps_eis_zoom_matching_frames_received, width, height, fps, if (newConfig.enableVideoStabilization) "ON" else "OFF", newConfig.zoomLevel)
                updateDisplay()
                Log.d(TAG, "Config switch confirmed by frame: ${width}x${height}")
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                currentConfigText = demoString(R.string.demo_switch_unconfirmed_no_x_frames_within_10_seconds, width, height, currentWidth, currentHeight)
                updateDisplay()
                Log.w(TAG, currentConfigText)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Config switch failed", e)
                tvInfo.text = demoString(R.string.demo_configuration_switch_failed, e.message)
            } finally {
                btnApply.isEnabled = true
            }
        }

        // 更新显示
        // Update the display.
        val orientationText = if (isPortrait) demoString(R.string.demo_portrait) else demoString(R.string.demo_landscape)
        currentConfigText = demoString(R.string.demo_switching_x_fps_waiting_for_frames, width, height, fps, orientationText)
        updateDisplay()

        // 隐藏配置面板
        // Hide the configuration panel.
        setConfigPanelVisible(false)
    }

    /**
     * 配置面板显示时移除外层按钮，避免遮挡 Spinner 的下拉菜单。
     * Hide the outer button while the configuration panel is visible so it does not obscure the Spinner dropdown.
     */
    private fun setConfigPanelVisible(visible: Boolean) {
        configPanel.visibility = if (visible) View.VISIBLE else View.GONE
        btnShowConfig.visibility = if (visible) View.GONE else View.VISIBLE
        tvInfo.visibility = if (visible) View.GONE else View.VISIBLE
        if (visible) {
            selectedConfigIndex = 0
            focusSelectedConfig()
        } else {
            configControls.forEach { it.isSelected = false }
        }
    }

    override fun handleGlassKeyEvent(keyEvent: Int): Boolean {
        if (configPanel.visibility != View.VISIBLE) {
            if (keyEvent == GlassKeyEvent.KEYCODE_CLICK && btnShowConfig.visibility == View.VISIBLE) {
                btnShowConfig.performClick()
                return true
            }
            return false
        }
        when (keyEvent) {
            GlassKeyEvent.KEYCODE_FRONT -> moveConfigSelection(1)
            GlassKeyEvent.KEYCODE_BEHIND -> moveConfigSelection(-1)
            GlassKeyEvent.KEYCODE_CLICK -> activateSelectedConfig()
            else -> return false
        }
        return true
    }

    private fun moveConfigSelection(offset: Int) {
        selectedConfigIndex =
            (selectedConfigIndex + offset + configControls.size) % configControls.size
        focusSelectedConfig()
    }

    private fun focusSelectedConfig() {
        updateConfigHighlight()
        val selected = configControls[selectedConfigIndex]
        selected.post {
            if (configPanel.visibility == View.VISIBLE) {
                selected.requestRectangleOnScreen(Rect(0, 0, selected.width, selected.height), true)
            }
        }
    }

    private fun updateConfigHighlight() {
        configControls.forEachIndexed { index, view ->
            view.isSelected = index == selectedConfigIndex
        }
    }

    /**
     * 单击直接轮换配置值，避免打开 Spinner 后眼镜腿方向键被弹窗消费。
     * Cycle configuration values directly on a click so opening the Spinner does not let its popup consume the temple direction keys.
     */
    private fun activateSelectedConfig() {
        when (selectedConfigIndex) {
            0 -> spinnerResolution.setSelection(
                (spinnerResolution.selectedItemPosition + 1) % spinnerResolution.count,
            )
            1 -> spinnerFps.setSelection(
                (spinnerFps.selectedItemPosition + 1) % spinnerFps.count,
            )
            2 -> {
                currentZoom = if (currentZoom >= seekbarZoom.max) 1 else currentZoom + 1
                seekbarZoom.progress = currentZoom
                tvZoomValue.text = currentZoom.toString()
            }
            3 -> switchEis.isChecked = !switchEis.isChecked
            else -> configControls[selectedConfigIndex].performClick()
        }
    }

    /**
     * 创建 NV21 Callback
     * Create the NV21 callback.
     * @param isPortrait 是否为竖屏模式
     * Whether portrait mode is active.
     */
    private fun createNv21Callback(
        isPortrait: Boolean = false,
        expected: CameraShareConfig? = null,
        firstFrame: CompletableDeferred<Unit>? = null,
    ): CameraShareHelper.Nv21Callback {
        val generation = callbackGeneration
        return object : CameraShareHelper.Nv21Callback {
            override fun onCameraOpened(width: Int, height: Int) {
                if (generation != callbackGeneration) return
                Log.d(TAG, "NV21 camera opened: ${width}x${height}, isPortrait=$isPortrait")
                currentWidth = width
                currentHeight = height
                if (currentConfigText.isEmpty()) {
                    val orientationText = if (isPortrait) demoString(R.string.demo_portrait) else demoString(R.string.demo_landscape)
                    currentConfigText = demoString(R.string.demo_configuration_x_eis_zoom, width, height, orientationText, if (currentEis) "ON" else "OFF", currentZoom)
                }
                activity?.runOnUiThread {
                    updateDisplay()
                }
            }

            override fun onNv21Frame(nv21: ByteArray, width: Int, height: Int, timestamp: Long) {
                if (generation != callbackGeneration) return
                currentWidth = width
                currentHeight = height
                if (expected != null && width == expected.previewWidth && height == expected.previewHeight) {
                    firstFrame?.complete(Unit)
                }
                // 记录帧间隔，便于观察采集帧率。
                // Record frame intervals to observe the capture frame rate.
                val now = System.currentTimeMillis()
                if (lastFrameTimestamp > 0) {
                    val interval = now - lastFrameTimestamp
                    frameIntervalSum += interval
                    frameIntervalCount++
                    if (frameIntervalCount % 30 == 0) {
                        val avgInterval = frameIntervalSum / frameIntervalCount
                        Log.d(TAG, "Frame interval stats: avg=${avgInterval}ms, target=${1000/FPS_OPTIONS[currentFpsIndex]}ms (${FPS_OPTIONS[currentFpsIndex]}fps), actual fps=${1000f/avgInterval}")
                        frameIntervalSum = 0
                        frameIntervalCount = 0
                    }
                }
                lastFrameTimestamp = now

                // 回调帧率与预览渲染解耦；预览较慢时只保留最新帧，不阻塞后续回调。
                // Decouple callback frame rate from preview rendering; keep only the latest frame when preview rendering is slower, without blocking later callbacks.
                updateFps(width, height)
                nv21SurfaceView.setPreviewData(nv21, width, height)
            }

            override fun onCameraClosed() {
                Log.d(TAG, "NV21 camera closed")
            }

            override fun onError(code: Int, msg: String) {
                if (generation != callbackGeneration) return
                firstFrame?.completeExceptionally(IllegalStateException("$code: $msg"))
                Log.e(TAG, "NV21 error: code=$code, msg=$msg")
                activity?.runOnUiThread {
                    tvInfo.text = demoString(R.string.demo_error, code, msg)
                }
            }

            override fun onNv21ExportResolutionChanged(width: Int, height: Int, appliedPreviewFps: Int) {
                if (generation != callbackGeneration) return
                Log.d(TAG, "onNv21ExportResolutionChanged: ${width}x${height}, fps=$appliedPreviewFps")
                currentWidth = width
                currentHeight = height
                activity?.runOnUiThread {
                    updateDisplay()
                }
            }

            override fun onNv21ExportRuntimeParamsChanged(appliedPreviewFps: Int, videoStabilizationEnabled: Boolean) {
                Log.d(TAG, "onNv21ExportRuntimeParamsChanged: fps=$appliedPreviewFps, EIS=$videoStabilizationEnabled")
                activity?.runOnUiThread {
                    updateDisplay()
                }
            }

            override fun onZoomLevelChanged(zoomLevel: Int) {
                Log.d(TAG, "Zoom level changed: $zoomLevel")
            }
        }
    }

    private fun startNv21Export() {
        if (!GlassSdk.isReady()) {
            Log.e(TAG, "GlassSdk not ready")
            return
        }

        // 使用默认配置启动（选择第一个分辨率，即最高分辨率）
        // Start with the default configuration, selecting the first and therefore highest resolution.
        val resolutionOption = resolutionOptions[currentResolutionIndex.coerceIn(0, resolutionOptions.size - 1)].second
        val initialConfig = CameraShareConfig(
            previewWidth = resolutionOption.first,
            previewHeight = resolutionOption.second,
            previewTargetFps = FPS_OPTIONS[currentFpsIndex],
            enableVideoStabilization = currentEis,
            zoomLevel = currentZoom,
        )

        val isPortrait = resolutionOption.third

        if (nv21Helper.isNv21Active()) {
            Log.w(TAG, "startNv21Export: already active, skip")
            return
        }
        nv21Helper.initNv21ExportWithConfig(
            enableMix = false,
            config = initialConfig,
            callback = createNv21Callback(isPortrait),
        )

        val orientationText = if (isPortrait) demoString(R.string.demo_portrait) else demoString(R.string.demo_landscape)
        currentConfigText = demoString(R.string.demo_configuration_x_fps_eis_zoom, initialConfig.previewWidth, initialConfig.previewHeight, initialConfig.previewTargetFps, orientationText, if (currentEis) "ON" else "OFF", initialConfig.zoomLevel)
    }

    private fun updateFps(width: Int, height: Int) {
        frameCount++
        val now = System.currentTimeMillis()
        if (lastFpsTime == 0L) lastFpsTime = now
        val elapsed = now - lastFpsTime
        if (elapsed >= 1000) {
            val callbackFps = frameCount * 1000f / elapsed
            val renderedTotal = nv21SurfaceView.renderedFrameCount
            val droppedTotal = nv21SurfaceView.droppedFrameCount
            val renderedFps = (renderedTotal - lastRenderedFrameCount) * 1000f / elapsed
            val droppedFrames = droppedTotal - lastDroppedFrameCount
            activity?.runOnUiThread {
                updateDisplay(callbackFps, renderedFps, droppedFrames)
            }
            frameCount = 0
            lastFpsTime = now
            lastRenderedFrameCount = renderedTotal
            lastDroppedFrameCount = droppedTotal
        }
    }

    private fun updateDisplay(
        callbackFps: Float? = null,
        renderedFps: Float? = null,
        droppedFrames: Long = 0,
    ) {
        val resolutionText = demoString(R.string.demo_connected_x, currentWidth, currentHeight)
        val fpsText = callbackFps?.let {
            demoString(R.string.demo_sdk_callback_fps_preview_rendering_fps_discarded_preview).format(
                it,
                renderedFps ?: 0f,
                droppedFrames,
            )
        } ?: ""
        activity?.runOnUiThread {
            tvInfo.text = if (fpsText.isNotEmpty()) {
                "$resolutionText\n$currentConfigText\n$fpsText"
            } else {
                "$resolutionText\n$currentConfigText"
            }
        }
    }
}
