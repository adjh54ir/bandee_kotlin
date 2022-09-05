package com.example.bandeekotlin.camera

import android.graphics.Bitmap
import android.os.Debug
import android.util.Base64.DEFAULT
import android.util.Base64.encodeToString
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.bandeekotlin.LumaListener
import com.example.bandeekotlin.`interface`.ImageApi
import com.example.bandeekotlin.camera.LuminosityAnalyzer.Companion.lastAnalyzedTimestamp
import com.example.bandeekotlin.model.PostImage
import com.example.bandeekotlin.utils.ApiConnUtils
import com.example.bandeekotlin.utils.CommonUtils
import com.example.bandeekotlin.model.ResponseCode
import com.example.bandeekotlin.utils.ApiConnUtils.retrofitConnection
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis


/**
 * CameraX에서 나온 이미지를 분석하는 클래스
 * @reference: https://developer.android.com/training/camerax/analyze?hl=ko
 */
class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {
    companion object {
        private const val TAG = "CameraXBasic"
        private var lastAnalyzedTimestamp = 0L
        private var fileList: ArrayList<MultipartBody.Part> = ArrayList()

    }

    /**
     * CameraX에서 출력되는 데이터를 ByteArray로 파싱
     */
    private fun ByteBuffer.toByteArray(): ByteArray {
        rewind()    // Rewind the buffer to zero
        val data = ByteArray(remaining())
        get(data)   // Copy the buffer into a byte array
        return data // Return the byte array
    }

    val currentTimestamp = System.currentTimeMillis();

    /**
     * CameraX의 이미지를 분석하는 함수
     */
    override fun analyze(image: ImageProxy) {

        val elapsed: Long = measureTimeMillis {


//            /**
//             * [CASE1] BASE64로 처리되는 방식
//             */
//            // 1. ImageProxy를 bitmap으로 변경
//            val bitmap: Bitmap = CommonUtils.imageProxyToBitmap(image)
//
//            // 2. Bitmap -> ByteArr 배열
//            val byteArrayOutputStream = ByteArrayOutputStream()
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
//            val byteArray = byteArrayOutputStream.toByteArray()
//
//            // 3. Base64 전환
//            val base64Str = encodeToString(byteArray, DEFAULT)
//            Log.d("base64 ::::::::: ", base64Str)
//
//            // 4. Retrofit2 기반의 POST 전송
//            val tempImageName = "image1"
//            val service: ImageApi = retrofitConnection()    // [API] 이미지 전송을 위한 Retrofit2 기반의 연결 함수
//
//            val formData = PostImage(base64Str, tempImageName)
//            service.postBase64(formData)
//                .enqueue(object : Callback<ResponseCode> {
//                    override fun onResponse(
//                        call: Call<ResponseCode>,
//                        response: Response<ResponseCode>
//                    ) {
//                        val result = response.body().toString()
//                        Log.d("API RESPONSE", result)
//                        Log.d("종료 시간 ::", "${System.currentTimeMillis()}")
//                    }
//
//                    override fun onFailure(call: Call<ResponseCode>, t: Throwable) {
//                        Log.e("ERROR_CALL", call.toString())
//                        Log.e("ERROR", t.toString())
//                    }
//                })

            /**
             * [CASE2] MULTI-PART 이용방식
             */
            // ========================
            // [STEP1] ImageProxy -> bitmap으로 변경 - 공통 함수 활용
            val bitmap: Bitmap = CommonUtils.imageProxyToBitmap(image)

            // [STEP2] Bitmap -> ByteArr 배열로 변경
            val byteArray = CommonUtils.bitmapToByteArr(bitmap)

            // [STEP3] ByteArr => Multipart 로 변경
            val fileName = "photo_${(1..50000).random()}.png"   // 랜덤 숫자 채번
            val requestFile = RequestBody.create(MediaType.parse("image/*"), byteArray)
            val body = MultipartBody.Part.createFormData("imageFile", fileName, requestFile)

            // [STPE4] List<MultipartBody> 형태로 데이터를 누적 시킴.
            fileList.add(body)
            Log.d("배열의 누적 체크", "${fileList.size}")

            // [STEP5] 배열을 누적시킨뒤 한번에 전송 기능
            if (fileList.size >= 1) {
                Log.d("전송 준비중!!!", "오예에에에에엥 시작!! ")

                // [STEP6] Multi-part를 이용한 데이터 전송
                ApiConnUtils.retrofitConnection().postImage(fileList)
                    .enqueue(
                        object : Callback<ResponseCode> {
                            override fun onResponse(
                                call: Call<ResponseCode>,
                                response: Response<ResponseCode>
                            ) {
                                val result = response.body().toString()
                                Log.d("API RESPONSE", result)
                            }

                            override fun onFailure(call: Call<ResponseCode>, t: Throwable) {
                                Log.e("ERROR_CALL", call.toString())
                                Log.e("ERROR", t.toString())
                            }
                        })

                Log.d("누적된 리스트를 확인하장 ~", "$fileList")
                // [STEP7] 5개가 완료되면 배열 비워주기
                fileList.clear();
            }

            /**
             * default Option
             */
            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()

            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            // 카메라 회전 각도 (카메라 이미지 회전이 아니라, 디바이스 retation 이다.)
            val rotationDegrees = image.imageInfo.rotationDegrees
            Log.d(TAG, "회전 각도 : $rotationDegrees")

            // 평균 이미지 광도 리스너 반환
            listener(luma)

            image.close() // 사용된 이미지 삭제
            Thread.sleep(300);     // 1초당 1개씩 2개씩 처리하도록 변경
        }

        Log.d("수행시간을 체크합니당~~~~~", "${elapsed}")
    }
}
