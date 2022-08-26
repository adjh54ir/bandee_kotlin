package com.example.bandeekotlin

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Base64
import android.util.Base64.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.bandeekotlin.*
import com.example.bandeekotlin.`interface`.ImageApi
import com.example.bandeekotlin.model.PostImage
import com.example.bandeekotlin.model.ResponseCode
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.*
import java.util.*


class MainActivity : AppCompatActivity() {

    // 시스템 환경설정 - 네트워크에 나와있는 IP주소에 따라서 지정을 한다.
//    private val API_BASE_URL = "http://10.2.2.0:5000"       // 시뮬레이터
    private val API_BASE_URL = "http://192.168.0.4:5000"  // 로컬 디바이스

    // 메인 페이지
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        createRetrofitApi(); // Retrofit2
//        okHttpClinentApi(); //  okHTTP
    }

    /**
     * Retrofit을 이용한 API 통신 테스트
     */
    private fun createRetrofitApi() {
        val imageService = Retrofit
            .Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        println("-- createRetrofitApi Loading !! -- ")
        val service = imageService.create(ImageApi::class.java)
        postBase64(service);
//        getBase64(service);
    }


    /**
     * image to Base64
     */
    private fun imageToBase64(): String {

        // 1. 실제 이미지 파일
        val image1 = R.drawable.image_2

        // 2. 실제 이미지 파일 -> Bitmap
        val drawable = getDrawable(image1)
        val bitmapDrawable = drawable as BitmapDrawable
        val imageToBitmap: Bitmap = bitmapDrawable.bitmap

        // 3. Bitmap -> ByteArr 배열
        val byteArrayOutputStream = ByteArrayOutputStream()
        imageToBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()

        val base64Str = Base64.encodeToString(byteArray, Base64.DEFAULT);
        Log.d("base64 ::::::::: ", base64Str)

        return base64Str
    }


    /**
     * POST 방식으로 데이터 전송
     */
    private fun postBase64(service: ImageApi) {
        val base64Str = imageToBase64();
        // Image To base64
        val formData = PostImage(base64Str)
        service.postBase64(formData)
            .enqueue(object : retrofit2.Callback<ResponseCode> {
                override fun onResponse(
                    call: retrofit2.Call<ResponseCode>,
                    response: Response<ResponseCode>
                ) {
                    val result = response.body().toString();
                    Log.d("API RESPONSE", result)
                }

                override fun onFailure(call: retrofit2.Call<ResponseCode>, t: Throwable) {
                    Log.e("ERROR_CALL", call.toString())
                    Log.e("ERROR", t.toString())
                }
            });

    }


    /**
     * GET 방식을 통한 데이터 전달 방식
     */
    private fun getBase64(service: ImageApi) {
        service.getBase64(imageBase64 = "asdsd")
            .enqueue(object : retrofit2.Callback<ResponseCode> {
                // 응답을 받는 경우
                override fun onResponse(
                    call: retrofit2.Call<ResponseCode>,
                    response: Response<ResponseCode>
                ) {
                    // 200 / 300
                    if (response.isSuccessful) {
                        val result = response.body().toString();
                        Log.d("API RESPONSE", "${result}")
                    } else {
                        Log.e("API FAIL", "실패입니다!!!!")
                    }
                }

                // 응답에 실패하는 경우
                override fun onFailure(call: retrofit2.Call<ResponseCode>, t: Throwable) {
                    Log.e("ERROR_CALL", call.toString())
                    Log.e("ERROR", t.toString())
                }
            });
    }

    /**
     * OKHTTP를 이용한 API 연결 방식
     */
    private fun okHttpClinentApi() {
        val okHttpClient = OkHttpClient()
        val request: okhttp3.Request = okhttp3.Request.Builder().url("$API_BASE_URL/").build()

        okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("ERROR_CALL", call.toString())
                Log.e("ERROR", e.toString())
            }

            @SuppressLint("LongLogTag")
            @Throws(IOException::class)  // called if we get a
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val result = response.body?.toString()
                Log.d("안녕하세요 제발 수행되면 저에게 이야기좀 해주세요", "결과는 ${result}")
            }
        })

    }

}
