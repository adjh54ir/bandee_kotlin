package com.example.bandeekotlin.controller

import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

typealias LumaListener = (luma: Double) -> Unit

/**
 * 이미지 분석이 가능하다.
 */
private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    override fun analyze(image: ImageProxy) {

        val buffer = image.planes[0].buffer
        val data = buffer.toByteArray()
        val pixels = data.map { it.toInt() and 0xFF }
        val luma = pixels.average()

        // 카메라 회전 각도 (카메라 이미지 회전이 아니라, 디바이스 retation 이다.)
        val rotationDegrees = image.imageInfo.rotationDegrees
        Log.d("TAG", "회전 각도 : $rotationDegrees")

        // 평균 이미지 광도 리스너 반환
        listener(luma)

        image.close()
    }
}