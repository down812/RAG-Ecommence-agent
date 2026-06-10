package com.jschaofan.ragagent.ui.chat.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.jschaofan.ragagent.ui.chat.model.PreparedChatImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import kotlin.math.max

/**
 * 把系统 Uri 转换为聊天接口可上传的缓存图片。
 *
 * 处理过程包含采样解码、方向纠正、尺寸限制和 JPEG 压缩，避免直接上传相机原图。
 */
class ImageAttachmentProcessor(
    private val context: Context,
) {
    suspend fun prepare(uri: Uri): Result<PreparedChatImage> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decodeSampledBitmap(uri)
            val rotatedBitmap = rotateByExif(bitmap, uri)
            val scaledBitmap = rotatedBitmap.scaleToFit(MAX_IMAGE_EDGE)
            val outputFile = createOutputFile()

            try {
                compressToFile(scaledBitmap, outputFile)
                require(outputFile.length() <= MAX_FILE_BYTES) {
                    "图片处理后仍然过大，请选择尺寸更小的图片"
                }
            } catch (error: Throwable) {
                outputFile.delete()
                throw error
            } finally {
                if (scaledBitmap !== rotatedBitmap) scaledBitmap.recycle()
                if (rotatedBitmap !== bitmap) rotatedBitmap.recycle()
                bitmap.recycle()
            }

            PreparedChatImage(
                id = UUID.randomUUID().toString(),
                file = outputFile,
                mediaType = JPEG_MEDIA_TYPE,
                fingerprint = outputFile.sha256(),
            )
        }
    }

    /**
     * 创建相机输出目标。FileProvider Uri 可安全授权给外部相机应用写入。
     */
    fun createCameraTarget(): CameraCaptureTarget {
        val directory = File(context.cacheDir, CAMERA_DIRECTORY).apply { mkdirs() }
        val file = File(directory, "capture-${UUID.randomUUID()}.jpg")
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        return CameraCaptureTarget(file = file, uri = uri)
    }

    fun discardCameraTarget(target: CameraCaptureTarget) {
        target.file.delete()
    }

    private fun decodeSampledBitmap(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取所选图片" }
            BitmapFactory.decodeStream(input, null, bounds)
        }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "所选文件不是有效图片" }

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(
                width = bounds.outWidth,
                height = bounds.outHeight,
            )
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取所选图片" }
            requireNotNull(BitmapFactory.decodeStream(input, null, options)) {
                "图片解码失败"
            }
        }
    }

    private fun rotateByExif(bitmap: Bitmap, uri: Uri): Bitmap {
        val orientation = context.contentResolver.openInputStream(uri).use { input ->
            input?.let { ExifInterface(it).rotationDegrees } ?: 0
        }
        if (orientation == 0) return bitmap

        val matrix = Matrix().apply { postRotate(orientation.toFloat()) }
        return Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true,
        )
    }

    private fun Bitmap.scaleToFit(maxEdge: Int): Bitmap {
        val currentMaxEdge = max(width, height)
        if (currentMaxEdge <= maxEdge) return this

        val ratio = maxEdge.toFloat() / currentMaxEdge
        return Bitmap.createScaledBitmap(
            this,
            (width * ratio).toInt(),
            (height * ratio).toInt(),
            true,
        )
    }

    private fun compressToFile(bitmap: Bitmap, outputFile: File) {
        var quality = INITIAL_JPEG_QUALITY
        do {
            FileOutputStream(outputFile, false).use { output ->
                check(bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)) {
                    "图片压缩失败"
                }
            }
            quality -= JPEG_QUALITY_STEP
        } while (outputFile.length() > MAX_FILE_BYTES && quality >= MIN_JPEG_QUALITY)
    }

    private fun createOutputFile(): File {
        val directory = File(context.cacheDir, ATTACHMENT_DIRECTORY).apply { mkdirs() }
        return File(directory, "chat-${UUID.randomUUID()}.jpg")
    }

    private fun calculateSampleSize(width: Int, height: Int): Int {
        var sampleSize = 1
        while (max(width / sampleSize, height / sampleSize) > DECODE_MAX_EDGE) {
            sampleSize *= 2
        }
        return sampleSize
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().buffered().use { input ->
            val buffer = ByteArray(HASH_BUFFER_SIZE)
            while (true) {
                val readCount = input.read(buffer)
                if (readCount <= 0) break
                digest.update(buffer, 0, readCount)
            }
        }
        return digest.digest().joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }
    }

    private companion object {
        const val CAMERA_DIRECTORY = "camera"
        const val ATTACHMENT_DIRECTORY = "chat-images"
        const val JPEG_MEDIA_TYPE = "image/jpeg"
        const val MAX_IMAGE_EDGE = 1600
        const val DECODE_MAX_EDGE = 2400
        const val INITIAL_JPEG_QUALITY = 86
        const val MIN_JPEG_QUALITY = 56
        const val JPEG_QUALITY_STEP = 10
        const val MAX_FILE_BYTES = 5L * 1024 * 1024
        const val HASH_BUFFER_SIZE = 8 * 1024
    }
}

data class CameraCaptureTarget(
    val file: File,
    val uri: Uri,
)
