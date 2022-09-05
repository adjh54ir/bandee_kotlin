package com.example.bandeekotlin.utils

import android.annotation.SuppressLint
import android.util.Log
import com.example.bandeekotlin.`interface`.ImageApi
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

object ApiConnUtils {

    private const val API_BASE_URL = "http://192.168.0.4:5000"  // 로컬 디바이스

    /**
     * [API Util] 'OKHTTP'를 이용한 API 연결 방식
     */
    private fun okHttpClientApi() {
        val okHttpClient = OkHttpClient()
        val request: Request = Request.Builder().url("$API_BASE_URL/").build()

        okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                Log.e("ERROR_CALL", call.toString())
                Log.e("ERROR", e.toString())
            }

            @SuppressLint("LongLogTag")
            @Throws(IOException::class)  // called if we get a
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
            }
        })
    }

    /**
     * [API Util] 이미지 전송을 위한 Retrofit2 기반의 연결 함수
     */
    fun retrofitConnection(): ImageApi {
        val imageService = Retrofit
            .Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return imageService.create(ImageApi::class.java)
    }

}