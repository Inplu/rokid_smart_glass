package com.rokid.glass.view

import com.rokid.glesse.R

import com.rokid.glass.utils.demoString

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min

class TestGridView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    enum class PreviewScaleType {
        // 完整显示摄像头画面，可能有黑边
        // Display the entire camera image, possibly with black bars.
        FIT_CENTER,
        // 铺满屏幕，可能裁剪摄像头画面
        // Fill the screen, possibly cropping the camera image.
        CENTER_CROP
    }

    /**
     * 摄像头原始宽高
     * Original camera width and height.
     */
    var cameraWidth = 1080
    var cameraHeight = 1920

    /**
     * 如果摄像头画面在预览中旋转了 90 度，需要设置为 true。
     * Set this to true if the camera image is rotated by 90 degrees in the preview.
     *
     * 眼镜屏幕是 480x640 竖屏，
     * The glasses screen uses a portrait resolution of 480 x 640,
     * 摄像头是 1920x1080 横屏，
     * The camera uses a landscape resolution of 1920 x 1080,
     * 实际预览中大概率会旋转成 1080x1920 再显示。
     * The actual preview will likely be rotated to 1080 x 1920 before display.
     */
    var cameraRotated90 = true

    /**
     * 根据你的预览方式选择：
     * Choose according to the preview mode:
     *
     * 如果预览铺满整个眼镜屏幕，通常是 CENTER_CROP。
     * A preview that fills the entire glasses screen usually uses CENTER_CROP.
     * 如果预览完整显示但有黑边，通常是 FIT_CENTER。
     * A fully visible preview with black bars usually uses FIT_CENTER.
     */
    var previewScaleType = PreviewScaleType.CENTER_CROP

    private val screenGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val screenBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLUE
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GREEN
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val cameraRectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    private val cameraGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.YELLOW
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
        alpha = 160
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 14f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val screenW = width.toFloat()
        val screenH = height.toFloat()

        drawScreenGrid(canvas, screenW, screenH)
        drawCameraMappingGrid(canvas, screenW, screenH)
    }

    private fun drawScreenGrid(canvas: Canvas, screenW: Float, screenH: Float) {
        // 1. 画屏幕边框，实际就是眼镜显示区域
        // 1. Draw the screen border, which represents the glasses display area.
        canvas.drawRect(0f, 0f, screenW, screenH, screenBorderPaint)

        // 2. 画屏幕网格，480x640 建议每 80px 一格
        // 2. Draw the screen grid; an 80 px cell size is recommended for 480 x 640.
        val gridSize = 80f

        var x = 0f
        while (x <= screenW) {
            canvas.drawLine(x, 0f, x, screenH, screenGridPaint)
            canvas.drawText("x=${x.toInt()}", x + 4f, 24f, textPaint)
            x += gridSize
        }

        var y = 0f
        while (y <= screenH) {
            canvas.drawLine(0f, y, screenW, y, screenGridPaint)
            canvas.drawText("y=${y.toInt()}", 4f, y + 24f, textPaint)
            y += gridSize
        }

        // 3. 画中心线
        // 3. Draw the center lines.
        val centerX = screenW / 2f
        val centerY = screenH / 2f

        canvas.drawLine(centerX, 0f, centerX, screenH, centerPaint)
        canvas.drawLine(0f, centerY, screenW, centerY, centerPaint)

        canvas.drawCircle(centerX, centerY, 8f, centerPaint)

        canvas.drawText(demoString(R.string.demo_screen_x_status_30, screenW.toInt(), screenH.toInt()), 10f, screenH - 20f, textPaint)
        canvas.drawText(demoString(R.string.demo_center_status_31, centerX.toInt(), centerY.toInt()), centerX + 10f, centerY - 10f, textPaint)
    }

    private fun drawCameraMappingGrid(canvas: Canvas, screenW: Float, screenH: Float) {
        val cameraDisplayRect = getCameraDisplayRect(screenW, screenH)

        // 摄像头画面映射到屏幕后的区域，CENTER_CROP 时可能超出屏幕
        // Area occupied by the camera image when mapped to the screen; CENTER_CROP may extend beyond the screen.
        canvas.drawRect(cameraDisplayRect, cameraRectPaint)

        canvas.drawText(
            "CAMERA AREA",
            cameraDisplayRect.left + 8f,
            maxOf(cameraDisplayRect.top + 18f, 18f),
            textPaint
        )

        val displayCameraW = if (cameraRotated90) {
            cameraHeight.toFloat()
        } else {
            cameraWidth.toFloat()
        }

        val displayCameraH = if (cameraRotated90) {
            cameraWidth.toFloat()
        } else {
            cameraHeight.toFloat()
        }

        val scaleX = cameraDisplayRect.width() / displayCameraW
        val scaleY = cameraDisplayRect.height() / displayCameraH

        canvas.save()

        // 只显示眼镜屏幕可见范围内的内容
        // Display only content within the visible area of the glasses screen.
        canvas.clipRect(0f, 0f, screenW, screenH)

        // 摄像头坐标分成 4 等份
        // Divide the camera coordinates into four equal sections.
        val cameraGridX = displayCameraW / 4f
        val cameraGridY = displayCameraH / 4f

        /**
         * 绘制 camX 竖线
         * Draw a vertical camX line.
         */
        var camX = 0f
        while (camX <= displayCameraW) {
            val screenX = cameraDisplayRect.left + camX * scaleX

            // 竖线
            // Vertical line.
            canvas.drawLine(
                screenX,
                cameraDisplayRect.top,
                screenX,
                cameraDisplayRect.bottom,
                cameraGridPaint
            )

            // 顶部 camX
            // camX label at the top.
            canvas.drawText(
                "camX=${camX.toInt()}",
                screenX + 3f,
                34f,
                textPaint
            )

            // 底部 camX，再加一个，方便眼镜里看
            // Add another camX label at the bottom for easier viewing through the glasses.
            canvas.drawText(
                "camX=${camX.toInt()}",
                screenX + 3f,
                screenH - 10f,
                textPaint
            )

            camX += cameraGridX
        }

        /**
         * 绘制 camY 横线
         * Draw a horizontal camY line.
         */
        var camY = 0f
        while (camY <= displayCameraH) {
            val screenY = cameraDisplayRect.top + camY * scaleY

            // 横线
            // Horizontal line.
            canvas.drawLine(
                cameraDisplayRect.left,
                screenY,
                cameraDisplayRect.right,
                screenY,
                cameraGridPaint
            )

            // 左侧 camY
            // camY label on the left.
            canvas.drawText(
                "camY=${camY.toInt()}",
                8f,
                screenY - 4f,
                textPaint
            )

            // 右侧 camY，再加一个，方便对比
            // Add another camY label on the right for comparison.
            canvas.drawText(
                "camY=${camY.toInt()}",
                screenW - 90f,
                screenY - 4f,
                textPaint
            )

            camY += cameraGridY
        }

        canvas.restore()

        // 显示映射信息，方便排查
        // Display mapping information to aid troubleshooting.
        canvas.drawText(
            "scaleX=${String.format("%.3f", scaleX)} scaleY=${String.format("%.3f", scaleY)}",
            8f,
            screenH - 42f,
            textPaint
        )

        canvas.drawText(
            "rect L=${cameraDisplayRect.left.toInt()} T=${cameraDisplayRect.top.toInt()} R=${cameraDisplayRect.right.toInt()} B=${cameraDisplayRect.bottom.toInt()}",
            8f,
            screenH - 26f,
            textPaint
        )
    }

    private fun getCameraDisplayRect(screenW: Float, screenH: Float): RectF {
        val displayCameraW = if (cameraRotated90) cameraHeight.toFloat() else cameraWidth.toFloat()
        val displayCameraH = if (cameraRotated90) cameraWidth.toFloat() else cameraHeight.toFloat()

        val scale = when (previewScaleType) {
            PreviewScaleType.FIT_CENTER -> {
                min(screenW / displayCameraW, screenH / displayCameraH)
            }

            PreviewScaleType.CENTER_CROP -> {
                max(screenW / displayCameraW, screenH / displayCameraH)
            }
        }

        val scaledW = displayCameraW * scale
        val scaledH = displayCameraH * scale

        val left = (screenW - scaledW) / 2f
        val top = (screenH - scaledH) / 2f
        val right = left + scaledW
        val bottom = top + scaledH

        return RectF(left, top, right, bottom)
    }
}