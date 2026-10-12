package com.rokid.glass.camera

/**
 * 接收相机共享页面由 Activity 统一分发的眼镜腿手势。
 * Receive temple gestures that the Activity centrally dispatches to camera-sharing pages.
 */
interface CameraShareGestureHandler {
    fun handleGlassKeyEvent(keyEvent: Int): Boolean
}
