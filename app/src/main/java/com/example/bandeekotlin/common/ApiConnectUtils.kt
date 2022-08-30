package com.example.bandeekotlin.common

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getDrawable
import com.example.bandeekotlin.R
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

object ApiConnectUtils {


    private val API_BASE_URL = "http://192.168.0.4:5000"  // 로컬 디바이스

    /**
     * OKHTTP를 이용한 API 연결 방식
     */
    private fun okHttpClinentApi() {
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
//                val result = response.body?.toString()
//                Log.d("안녕하세요 제발 수행되면 저에게 이야기좀 해주세요", "결과는 ${result}")
            }
        })
    }

}