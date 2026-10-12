package com.rokid.glass.camera

import java.io.InputStream
import java.nio.ByteBuffer

class ByteBufferInputStream(private val buffer: ByteBuffer) : InputStream() {

    /**
     * 读取单个字节
     * Read one byte.
     * @return 字节值（0-255），若已读完返回 -1
     * The byte value (0-255), or -1 if all data has been read.
     */
    override fun read(): Int {
        return if (buffer.hasRemaining()) {
            // 将字节转换为无符号整数（0-255）
            // Convert the byte to an unsigned integer (0-255).
            buffer.get().toInt() and 0xFF
        } else {
            // 已到达缓冲区末尾
            // The end of the buffer has been reached.
            -1
        }
    }

    /**
     * 批量读取字节到数组
     * Read a batch of bytes into an array.
     * @param b 目标字节数组
     * The destination byte array.
     * @param off 数组起始偏移量
     * The starting offset in the array.
     * @param len 最大读取长度
     * The maximum number of bytes to read.
     * @return 实际读取的字节数，若已读完返回 -1
     * The actual number of bytes read, or -1 if all data has been read.
     */
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (!buffer.hasRemaining()) {
            return -1
        }
        // 计算实际可读取的长度（不超过缓冲区剩余量和请求长度）
        // Calculate the readable length, limited by both the remaining buffer size and the requested length.
        val bytesToRead = minOf(len, buffer.remaining())
        buffer.get(b, off, bytesToRead)
        return bytesToRead
    }

    /**
     * 跳过指定字节数
     * Skip the specified number of bytes.
     * @param n 要跳过的字节数
     * The number of bytes to skip.
     * @return 实际跳过的字节数
     * The actual number of bytes skipped.
     */
    override fun skip(n: Long): Long {
        val skipBytes = minOf(n, buffer.remaining().toLong()).toInt()
        buffer.position(buffer.position() + skipBytes)
        return skipBytes.toLong()
    }

    /**
     * 查看剩余可读取的字节数
     * Get the number of remaining readable bytes.
     */
    override fun available(): Int = buffer.remaining()

    /**
     * 标记当前位置（ByteBuffer 本身支持标记，这里直接使用）
     * Mark the current position; ByteBuffer supports marking directly.
     */
    override fun mark(readlimit: Int) {
        buffer.mark()
    }

    /**
     * 重置到上次标记的位置
     * Reset to the last marked position.
     */
    override fun reset() {
        buffer.reset()
    }

    /**
     * 是否支持标记/重置操作（这里返回 true）
     * Whether mark/reset operations are supported; returns true here.
     */
    override fun markSupported(): Boolean = true
}
