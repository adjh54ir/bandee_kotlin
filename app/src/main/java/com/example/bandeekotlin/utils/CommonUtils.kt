package com.example.bandeekotlin.utils

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.example.bandeekotlin.*
import java.io.ByteArrayOutputStream
import android.content.Context

object CommonUtils {

    /**
     * 이미지 Proxy를 Bitmap으로 변경하는 함수
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

    /**
     * bitmap을 ByteArr 형태로 변경하는 함수
     */
    fun bitmapToByteArr(bitmap: Bitmap): ByteArray {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * image to Base64
     * STEP1: 내 디렉토리 내에서 실제 이미지 파일을 받는다
     * STEP2: 이미지 파일을 Bitmap 형태로 변경한다
     * STEP3: Bitmap -> ByteArr로 변경
     * STEP4: ByteArr -> String으로 변경
     */
    fun imageToBase64(context: Context): String {

        // 1. 실제 이미지 파일
        val image1: Drawable? = ContextCompat.getDrawable(context, R.drawable.image_3)

        // 2. 실제 이미지 파일 -> Bitmap
        val bitmapDrawable = image1 as BitmapDrawable
        val imageToBitmap: Bitmap = bitmapDrawable.bitmap

        // 3. Bitmap -> ByteArr 배열
        val byteArrayOutputStream = ByteArrayOutputStream()
        imageToBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }


}