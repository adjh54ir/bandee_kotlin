package com.example.bandeekotlin

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.bandeekotlin.`interface`.ImageApi
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    // 시스템 환경설정 - 네트워크에 나와있는 IP주소에 따라서 지정을 한다.
//    private val API_BASE_URL = "http://10.2.2.0:5000"   // 시뮬레이터를 사용하는 경우에는 이와 같이 사용한다.
    private val API_BASE_URL = "http://192.168.0.4:5000"

//    lateinit var page : TextVi


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
            .enqueue(object : retrofit2.Callback<JSONObject> {
                // 응답을 받는 경우
                override fun onResponse(
                    call: retrofit2.Call<JSONObject>,
                    response: retrofit2.Response<JSONObject>
                ) {
                    Log.d("오잉", "${response}")

                    if (response.isSuccessful) {
                        val result = response.body().toString();
                        Log.d("Reponse 결과값",  "${result}")
                    } else {
                        Log.e("FAILLLLLLL", "실패입니다!!!!")
                    }
                }

                // 응답에 실패하는 경우
                override fun onFailure(call: retrofit2.Call<JSONObject>, t: Throwable) {
                    Log.e("ERROR_CALL", call.toString())
                    Log.e("ERROR", t.toString())
                }
            });
    }

//    private fun okHttpClinentApi() {
//        val okHttpClient = OkHttpClient()
//        val request: Request = Request.Builder().url("$API_BASE_URL/").build()
//
//        okHttpClient.newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.e("ERROR_CALL", call.toString())
//                Log.e("ERROR", e.toString())
//            }
//
//            @SuppressLint("LongLogTag")
//            @Throws(IOException::class)  // called if we get a
//            override fun onResponse(call: Call, response: Response) {
//                val result = response.body?.toString()
//                Log.d("안녕하세요 제발 수행되면 저에게 이야기좀 해주세요", "결과는 ${result}")
//            }
//        })
//
//    }

}
