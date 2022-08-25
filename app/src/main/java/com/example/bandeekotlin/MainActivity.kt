package com.example.bandeekotlin

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.bandeekotlin.`interface`.ImageApi
import com.example.bandeekotlin.model.ResponseCode
import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

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
        val service = imageService.create(ImageApi::class.java)
        println("-- createRetrofitApi  Start!! -- ")
        /**
         * GET 방식을 통한 데이터 전달 방식
         * 당연히 안될껄 알고 있어~~~
         */
        service.getBase64(imageBase64 = "324o87234023894")
            .enqueue(object : retrofit2.Callback<ResponseCode> {
                // 응답을 받는 경우
                override fun onResponse(
                    call: Call<ResponseCode>,
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
