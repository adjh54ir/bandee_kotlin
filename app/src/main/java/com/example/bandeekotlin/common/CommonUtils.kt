package com.example.bandeekotlin.common

import android.graphics.*
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

interface CommonUtils {
    /**
     * 이미지 Proxy를 Bitmap으로 변경
     */
    fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val planeProxies = imageProxy.planes
        val yBuffer = planeProxies[0].buffer
        val uBuffer = planeProxies[1].buffer
        val vBuffer = planeProxies[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        val nv21 = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]
        uBuffer[nv21, ySize + vSize, uSize]
        val yuvImage =
            YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
        val imageBytes = out.toByteArray()
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}