package com.example.bandeekotlin

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Base64.*
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bandeekotlin.`interface`.ImageApi
import com.example.bandeekotlin.model.PostImage
import com.example.bandeekotlin.model.ResponseCode
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.*
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue


class MainActivity : AppCompatActivity() {
    // 시스템 환경설정 - 네트워크에 나와있는 IP주소에 따라서 지정을 한다.
    private val API_BASE_URL = "http://192.168.0.4:5000"  // 로컬 디바이스

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    }

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExcutor: ExecutorService

    // 메인 페이지
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//        createRetrofitApi(); // Retrofit2
        connectionCamera();
    }

    /**
     * 카메라 연결
     */
    private fun connectionCamera() {

        // [STEP1] 카메라 권한 체크
        if (allPermissionGranted()) {
            startCamera()
        } else {
            // 권한 요청
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSION, REQUEST_CODE_PERMISSIONS)
        }
//        // 캡처 버튼
//        camera_capture_button.setOnClickListener { takePhoto() }

        // 저장할 디렉토리
        outputDirectory = getOutputDirectory()

        // single thread
        cameraExcutor = Executors.newSingleThreadExecutor()
    }

    /**
     * 권한 승인 결과
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "권한 승인을 하지 않았습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    /**
     * 카메라 시작
     */
    private fun startCamera() {
    }

    /**
     * 권한 체크
     */
    private fun allPermissionGranted() = REQUIRED_PERMISSION.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply {
                mkdir()
            }
        }
        return if (mediaDir != null && mediaDir.exists()) {
            mediaDir
        } else {
            filesDir
        }
    }


    // ============================================================================================

    /**
     * Retrofit을 이용한 API 통신 테스트
     */
    @OptIn(ExperimentalTime::class)
    private fun createRetrofitApi() {
        val imageService = Retrofit
            .Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = imageService.create(ImageApi::class.java)
        // API 로 반복 통신 수행
        val mt = measureTimedValue {
            // 1초에 2번씩 수행
            for (i in 1..10) {
                Log.d("시간 시간 ::", "${System.currentTimeMillis()}");
                postBase64(service);        // API 호출
            }
        }
        Log.d("측정 시간!!!!", "${mt}");
    }


    /**
     * image to Base64
     * STEP1: 내 디렉토리 내에서 실제 이미지 파일을 받는다
     * STEP2: 이미지 파일을 Bitmap 형태로 변경한다
     * STEP3: Bitmap -> ByteArr로 변경
     * STEP4: ByteArr -> String으로 변경
     */
    private fun imageToBase64(): String {

        // 1. 실제 이미지 파일
        val image1 = R.drawable.image_3

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
        val tempImageName = "image1";
        // Image To base64
        val formData = PostImage(base64Str, tempImageName)
        service.postBase64(formData)
            .enqueue(object : retrofit2.Callback<ResponseCode> {
                override fun onResponse(
                    call: retrofit2.Call<ResponseCode>,
                    response: Response<ResponseCode>
                ) {
                    val result = response.body().toString();
                    Log.d("API RESPONSE", result)

                    Log.d("종료 시간 ::", "${System.currentTimeMillis()}");
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
