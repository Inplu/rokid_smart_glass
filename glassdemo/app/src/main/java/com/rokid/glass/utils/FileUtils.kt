package com.rokid.glass.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import android.view.TextureView
import com.rokid.glass.utils.Scopes.workScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {
    private const val TAG = "FileUtils"

    // 图片统一保存到系统公共 Pictures 目录。
    // Save images in the system public Pictures directory.
    private val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

    // Demo 图片存放在 Pictures/album 子目录。
    // Store demo images in the Pictures/album subdirectory.
    private val albumDir = File(baseDir, "album")

    fun saveBitmap(bitmap: Bitmap) {
        workScope.launch {
            if (!albumDir.exists()) {
                albumDir.mkdirs()
            }
            // 使用时间戳避免覆盖已有图片。
            // Use a timestamp to avoid overwriting existing images.
            val timeStamp = SimpleDateFormat("MMdd_HHmmss", Locale.getDefault()).format(Date())
            val outputFile = File(albumDir, "IMG_$timeStamp.jpg")
            FileOutputStream(outputFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            }
            Log.d(TAG, "图片宽度: ${bitmap.width} ,图片高度: ${bitmap.height}  ${Thread.currentThread().name}")
        }
    }


    /**
     * 从 assets 目录读取图片文件
     * Read an image file from the assets directory.
     * @param context Context 对象
     * The Context object.
     * @param fileName assets 目录下的文件名（包含扩展名，如 "gonglu.png"）
     * The file name in assets, including its extension, for example "gonglu.png".
     * @return Bitmap 对象，如果读取失败则返回 null
     * The Bitmap, or null if reading fails.
     */
    fun loadBitmapFromAssets(context: Context, fileName: String): Bitmap? {
        return try {
            context.assets.open(fileName).use { inputStream ->
                BitmapFactory.decodeStream(inputStream)
            }
        } catch (e: IOException) {
            Log.e(TAG, "从 assets 读取图片失败：$fileName", e)
            null
        }
    }

    /**
     * 定期读取 TextureView 当前帧并保存为图片。
     * Periodically capture the current TextureView frame and save it as an image.
     */
    fun handlerBitmap(textureView: TextureView) {
        workScope.launch {
            // 每秒读取一次预览帧。
            // Read one preview frame per second.
            infiniteIntervalFlow(1000).collect { value ->
                if (!textureView.isAvailable) {
                    return@collect
                }
                val bitmap: Bitmap? = textureView.bitmap
                if (bitmap == null) {
                    return@collect
                }
                // 与 saveBitmap 使用相同的公共图片目录。
                // Use the same public pictures directory as saveBitmap.
                val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val albumDir = File(baseDir, "album")
                if (!albumDir.exists()) {
                    albumDir.mkdirs()
                }
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val outputFile = File(albumDir, "IMG_$timeStamp.jpg")
                FileOutputStream(outputFile).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                }
                Log.d(TAG, "图片宽度: ${bitmap.width} ,图片高度: ${bitmap.height}  ${Thread.currentThread().name}")
            }
        }
    }

    /**
     * 按指定间隔持续发出递增序号。
     * Continuously emit increasing sequence numbers at the specified interval.
     * @param intervalMillis 发送间隔，单位为毫秒
     * Emission interval in milliseconds.
     */
    fun infiniteIntervalFlow(intervalMillis: Long = 100): Flow<Long> = flow {
        var counter = 0L
        while (true) {
            emit(counter)
            counter++
            delay(intervalMillis)
        }
    }

}
