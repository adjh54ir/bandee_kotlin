package com.example.bandeekotlin.utils

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.Base64
import androidx.camera.core.ImageProxy
import androidx.core.content.ContextCompat
import com.example.bandeekotlin.*
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import org.json.JSONArray
import java.io.*

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
    fun imageToBase64(): String {

        // 1. 실제 이미지 파일
        val image1: Drawable? = ContextCompat.getDrawable(MainActivity.context(), R.drawable.image_3)

        // 2. 실제 이미지 파일 -> Bitmap
        val bitmapDrawable = image1 as BitmapDrawable
        val imageToBitmap: Bitmap = bitmapDrawable.bitmap

        // 3. Bitmap -> ByteArr 배열
        val byteArrayOutputStream = ByteArrayOutputStream()
        imageToBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    /**
     * JSONArray -> file 데이터 변환 및 저장
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun convertJsonTofile(
        paramJsonArray: JSONArray,
        fileName: String,
        saveLoc: String
    ) {
        var dir = ""
        // STEP1: 저장 경로
        if (saveLoc == "files") dir = File(MainActivity.context().filesDir.absolutePath + "/$fileName").toString()
        if (saveLoc == "cache") dir = File(MainActivity.context().cacheDir.absolutePath + "/$fileName").toString()

        // STEP3: JSON Array -> file 형태로 구성
        try {
            // 2. 동일한 이름 일 경우 데이터가 이어져서 넘어가는 문제가 발생하여 오늘 날짜를 더함.
            var filename: String = "$fileName.json"
            val writer = FileWriter(dir, true)
            // 쓰기 속도 향상
            val buffer = BufferedWriter(writer)
            buffer.write(paramJsonArray.toString())
            buffer.close()
            Log.d("success", "JSON 파일이 생성되었습니다.")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("error", "파일 생성을 종료합니다.")
        }
    }



}