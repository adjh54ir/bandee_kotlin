package com.example.bandeekotlin

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.system.Os.mkdir
import android.util.Base64.*
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bandeekotlin.*
import com.example.bandeekotlin.`interface`.ImageApi
import com.example.bandeekotlin.common.CommonUtils
import com.example.bandeekotlin.model.PostImage
import com.example.bandeekotlin.model.ResponseCode
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import java.io.*
import java.lang.Thread.sleep
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.time.ExperimentalTime


private const val TAG = "CameraXBasic"

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
//        connectionCamera(); // 카메라 연결
//        createRetrofitApi(); // Retrofit2 연결
        sendMultiPart();
    }




    /**
     * Multi-part를 이용하여 이미지 전송
     */
    private fun sendMultiPart() {

        // 여러 file들을 담아줄 ArrayList
        val fileList: ArrayList<MultipartBody.Part> = ArrayList()

        for (i: Int in 1..10) {
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

            var fileName: String = "photo$i.png"
            val requestFile = RequestBody.create(MediaType.parse("image/*"), byteArray)
            val body = MultipartBody.Part.createFormData("imageFile", fileName, requestFile);
            Log.d("Multipart !!!!!!!!!", "${body}")

            fileList.add(body)
        }


        val imageService = Retrofit
            .Builder()
            .baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val service = imageService.create(ImageApi::class.java)

        service.postImage(fileList)
            .enqueue(
                object : Callback<ResponseCode> {
                    override fun onResponse(
                        call: Call<ResponseCode>,
                        response: Response<ResponseCode>
                    ) {
                        val result = response.body().toString();
                        Log.d("API RESPONSE", result)

                        Log.d("종료 시간 ::", "${System.currentTimeMillis()}");
                    }

                    override fun onFailure(call: Call<ResponseCode>, t: Throwable) {
                        Log.e("ERROR_CALL", call.toString())
                        Log.e("ERROR", t.toString())
                    }
                });
    }


    /**
     * [API] 최초 카메라 연결
     */
    private fun connectionCamera() {

        // [STEP1] 카메라 권한 체크
        if (allPermissionGranted()) {
            startCamera()
        } else {
            // 권한 요청
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSION, REQUEST_CODE_PERMISSIONS)
        }
        // 캡처 버튼
        val myButton: Button = findViewById(R.id.camera_capture_button)
        myButton.setOnClickListener { takePhoto() }

        // 저장할 디렉토리
        outputDirectory = getOutputDirectory()

        // single thread
        cameraExcutor = Executors.newSingleThreadExecutor()
    }

    /**
     * [API] 카메라 권한 승인 결과
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
     * [API] 카메라 시작
     */
    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable { // 카메라의 수명 주기를 LifecycleOwner응용 프로그램 프로세스 내에서 바인딩하는 데 사용됩니다 .

            // 카메라의 수명 주기를 수명 주기 소유자에게 바인딩하는 데 사용됩니다.
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {

                    // Preview객체를 초기화하고 빌드를 호출하고 뷰파인더에서 표면 공급자를 가져온 다음 미리보기에서 설정합니다.
                    val viewFinder: PreviewView = findViewById(R.id.viewFinder)
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            // 평균 이미지 광도
            var imageLuminosityAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExcutor, LuminosityAnalyzer { luma ->
                        Log.d(TAG, "평균 광도 : $luma")
                    })
                }

            // 기본 뒤쪽 카메라 선택
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // 사용한 binding 모두 해제
                cameraProvider.unbindAll()

                // bind 카메라
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageLuminosityAnalyzer
                )

            } catch (exc: Exception) {
                // 앱이 더 이상 포커스에 있지 않은 경우와 같이 이 코드가 실패할 수 있음
                Log.e(TAG, "bind 에러", exc)
            }

        }, ContextCompat.getMainExecutor(this)) // 메인 스레드 실행
    }

    /**
     * [API] 버튼을 눌러서 사진 찍기
     */
    private fun takePhoto() {

        // null인 경우 함수를 종료합니다.
        // 이미지 캡처가 설정되기 전에 사진 버튼을 탭하면 null이 됩니다.
        // return문이 없으면 앱이 충돌합니다
        val imageCapture = imageCapture ?: return

        // 이미지를 저장할 타임 스탬프 출력 파일 생성
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.KOREAN)
                .format(System.currentTimeMillis()) + "jpg"
        )

        // 파일 + 메타데이터를 포함하는 출력 옵션 객체 생성
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // 촬영에 성공하고, 저장됨을 사용자에게 알리기
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "사진 캡쳐 성공 : $savedUri"
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                }

                override fun onError(exception: ImageCaptureException) {
                    // 이미지 캡처에 실패하거나 이미지 캡처 저장에 실패한 경우 오류 사례를 추가하여 실패했음을 기록
                    Log.e(TAG, "사진 캡쳐 실패 : ${exception.message}", exception)
                }
            }
        )

    }

    /**
     * [API] 권한 체크
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
//        val mt = measureTimedValue {
//            // 1초에 2번씩 수행
//            for (i in 1..10) {
//                Log.d("시간 시간 ::", "${System.currentTimeMillis()}");
        postBase64(service);        // API 호출
//            }
//        }
//        Log.d("측정 시간!!!!", "${mt}");
    }

    /**
     * POST 방식으로 데이터 전송
     */
    private fun postBase64(service: ImageApi) {
        val base64Str = CommonUtils.imageToBase64(this);
        val tempImageName = "image1";
        // Image To base64
        val formData = PostImage(base64Str, tempImageName)
        service.postBase64(formData)
            .enqueue(object : Callback<ResponseCode> {
                override fun onResponse(
                    call: Call<ResponseCode>,
                    response: Response<ResponseCode>
                ) {
                    val result = response.body().toString();
                    Log.d("API RESPONSE", result)

                    Log.d("종료 시간 ::", "${System.currentTimeMillis()}");
                }

                override fun onFailure(call: Call<ResponseCode>, t: Throwable) {
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
            .enqueue(object : Callback<ResponseCode> {
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
                override fun onFailure(call: Call<ResponseCode>, t: Throwable) {
                    Log.e("ERROR_CALL", call.toString())
                    Log.e("ERROR", t.toString())
                }
            });
    }


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
            // 1. ImageProxy를 bitmap으로 변경
            val bitmap: Bitmap = CommonUtils.imageProxyToBitmap(image)

            // 2. Bitmap -> ByteArr 배열
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            // 3. Base64 전환
            val base64Str = encodeToString(byteArray, DEFAULT);
            Log.d("base64 ::::::::: ", base64Str)

            // 4. Retrofit2 기반의 POST 전송
            val tempImageName = "image1";
            val imageService = Retrofit
                .Builder()
                .baseUrl("http://192.168.0.4:5000")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            val service = imageService.create(ImageApi::class.java)

            val formData = PostImage(base64Str, tempImageName)
            service.postBase64(formData)
                .enqueue(object : Callback<ResponseCode> {
                    override fun onResponse(
                        call: Call<ResponseCode>,
                        response: Response<ResponseCode>
                    ) {
                        val result = response.body().toString();
                        Log.d("API RESPONSE", result)
                        sleep(5000)     // TODO: 타이머 지정 1초당 1건씩 수행
                        Log.d("종료 시간 ::", "${System.currentTimeMillis()}");
                    }

                    override fun onFailure(call: Call<ResponseCode>, t: Throwable) {
                        Log.e("ERROR_CALL", call.toString())
                        Log.e("ERROR", t.toString())
                    }
                });


            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            // 카메라 회전 각도 (카메라 이미지 회전이 아니라, 디바이스 retation 이다.)
            val rotationDegrees = image.imageInfo.rotationDegrees
            Log.d(TAG, "회전 각도 : $rotationDegrees")

            // 평균 이미지 광도 리스너 반환
            listener(luma)

            image.close()
        }

    }


}

typealias LumaListener = (luma: Double) -> Unit
