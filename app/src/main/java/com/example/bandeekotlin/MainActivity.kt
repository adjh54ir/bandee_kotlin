package com.example.bandeekotlin

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.bandeekotlin.camera.LuminosityAnalyzer
import com.example.bandeekotlin.model.PostImage
import com.example.bandeekotlin.model.ResponseCode
import com.example.bandeekotlin.utils.ApiConnUtils.retrofitConnection
import com.example.bandeekotlin.utils.CommonUtils
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity() {

    init{
        instance = this
    }
    /**
     * 공통 변수에 대해서 선언
     */
    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSION = arrayOf(Manifest.permission.CAMERA)
        private var imageCaptureConfig: ImageCapture? = null
        private lateinit var outputDirectory: File
        private lateinit var cameraExcutor: ExecutorService
        private const val TAG = "CameraXBasic"
        var instance: MainActivity? = null
        fun context() : Context {
            return instance!!.applicationContext
        }
    }

    /**
     * [init] 최초 수행 함수
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        connectionCamera(); // 카메라 연결 기능
//        sendMultiPart() // Multi-part를 이용하여 이미지 전송

//        convertJsonTofile()
    }


    /**
     * 내부 캐시 메모리에 저장하는 방법
     */
    private fun saveInternalCacheStorage(filename: String, data: String) {
        try {
            val file = File(cacheDir, "myCache")
            val outputStream = FileOutputStream(file)
            outputStream.write(data.toByteArray())
            outputStream.close()
            Log.d("data.toByteArray()", "saved")
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    /**
     * JSONArray 형태를 파일로 구성
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun convertJsonTofile() {
        // STEP0: 임시값 구성
        val base64Str = CommonUtils.imageToBase64()
        val tempAttention = 99;
        val jsonObject = JSONObject()
        var jsonArrayList = JSONArray()

        try {
            for (i: Int in 1..10) {
                // STEP1: JSON 구성
                jsonObject.put("base64", base64Str)
                jsonObject.put("attention", tempAttention);
                // STEP2: JSON Array 형태로 구성
                jsonArrayList.put(jsonObject)
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        // STEP3: JSON Array -> file 형태로 구성
        try {
            // 1. 오늘 날짜를 가져옴.
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
            val formatted = current.format(formatter)

            // 2. 동일한 이름 일 경우 데이터가 이어져서 넘어가는 문제가 발생하여 오늘 날짜를 더함.
            var filename: String = "file_${formatted}.json"
//            var filename: String = "file.json"

            // Files 경로에 폴더를 생성한다.
            val cacheDir = File(cacheDir.absolutePath + "/$filename")
            val fileDir = File(filesDir.absolutePath + "/$filename")
            val writer = FileWriter(fileDir, false)
            // 쓰기 속도 향상
            val buffer = BufferedWriter(writer)
            buffer.write(jsonArrayList.toString())
            buffer.close()
            Log.d(TAG, "JSON 파일이 생성되었습니다.")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "파일 생성을 종료합니다.")
        }

        this.fileReader(filesDir.absolutePath + "/file.json")

    }

    /**
     * JSON 파일 읽기
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun fileReader(filePath: String) {
        val fileDir = File(filePath)
        val reader = BufferedReader(FileReader(fileDir))
        reader.lines().forEach {
            Log.d("test", it)
        }
    }


    /**
     * [함수] CameraX 시작
     */
    private fun connectionCamera() {

        // [STEP1] 카메라 권한 체크
        if (allPermissionGranted()) {
            startCamera()   // 카메라 시작 함수 수행
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
     * [함수] 카메라 권한 승인 결과
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
     * [함수] 카메라 시작
     */
    private fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({ // 카메라의 수명 주기를 LifecycleOwner 응용 프로그램 프로세스 내에서 바인딩하는 데 사용됩니다 .

            // 카메라의 수명 주기를 수명 주기 소유자에게 바인딩하는 데 사용됩니다.
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // [기능1] 미리보기(Preview)의 Option Setting
            val previewConfig = Preview.Builder()
                .build()
                .also {
                    // Preview객체를 초기화하고 빌드를 호출하고 뷰파인더에서 표면 공급자를 가져온 다음 미리보기에서 설정합니다.
                    val viewFinder: PreviewView = findViewById(R.id.viewFinder)
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            // [기능2] 이미지 캡쳐의 Option Setting
            imageCaptureConfig = ImageCapture.Builder().build()

            // [기능3] 이미지 분석 Option Setting
            val imageLuminosityAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExcutor, LuminosityAnalyzer { luma  ->
                        Log.d(TAG, "평균 광도 : $luma")
                    });
                }
            // [기능4] 카메라 방향 Option Setting
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                // 사용한 binding 모두 해제
                cameraProvider.unbindAll()


                // bind 카메라 - 설정한 옵션에 대해서 라이프 사이클에 모두 세팅
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    previewConfig,
                    imageCaptureConfig,
                    imageLuminosityAnalyzer
                )
            } catch (exc: Exception) {
                // 앱이 더 이상 포커스에 있지 않은 경우와 같이 이 코드가 실패할 수 있음
                Log.e(TAG, "bind 에러", exc)
            }

        }, ContextCompat.getMainExecutor(this)) // 메인 스레드 실행
    }

    /**
     * [함수] 버튼을 눌러서 사진 찍기
     */
    private fun takePhoto() {
        // 이미지 캡처가 설정되기 전에 사진 버튼을 탭하면 null이 됩니다.
        // return문이 없으면 앱이 충돌합니다
        val imageCapture = imageCaptureConfig ?: return

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
     * [함수] 권한 체크
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
     * [함수] multipart data-form 방식을 이용하여 이미지 전송 기능 함수
     */
    private fun sendMultiPart() {
        // 여러 file들을 담아줄 ArrayList
        val fileList: ArrayList<MultipartBody.Part> = ArrayList()

        // 1. 실제 리소스 이미지 파일
        val image1 = R.drawable.image_3

        // 2. 실제 리소스 이미지 파일 -> Bitmap 변환
        val drawable = getDrawable(image1)
        val bitmapDrawable = drawable as BitmapDrawable
        val imageToBitmap: Bitmap = bitmapDrawable.bitmap

        // 3. Bitmap -> ByteArr 배열 변환
        var byteArray = CommonUtils.bitmapToByteArr(imageToBitmap)

        // 4. ByteArr -> Mutipart 형태로 변환
        for (i: Int in 1..10) {
            val fileName = "photo$i.png"
            val requestFile = RequestBody.create(MediaType.parse("image/*"), byteArray)
            val body = MultipartBody.Part.createFormData("imageFile", fileName, requestFile)
            Log.d("Multipart !!!!!!!!!", "$body")
            fileList.add(body)
        }

        // [API] 서비스 호출
        retrofitConnection().postImage(fileList)
            .enqueue(
                object : Callback<ResponseCode> {
                    override fun onResponse(
                        call: Call<ResponseCode>,
                        response: Response<ResponseCode>
                    ) {
                        val result = response.body().toString()
                        Log.d("API RESPONSE", result)
                        Log.d("종료 시간 ::", "${System.currentTimeMillis()}")
                    }

                    override fun onFailure(call: Call<ResponseCode>, t: Throwable) {
                        Log.e("ERROR_CALL", call.toString())
                        Log.e("ERROR", t.toString())
                    }
                })
    }

    /**
     * [함수] POST 방식으로 BASE64 데이터 전송 기능 함수
     */
    private fun postBase64() {


        val base64Str = CommonUtils.imageToBase64()
        val tempImageName = "image1"
        // Image To base64
        val formData = PostImage(base64Str, tempImageName)
        retrofitConnection().postBase64(formData)
            .enqueue(object : Callback<ResponseCode> {
                override fun onResponse(
                    call: Call<ResponseCode>,
                    response: Response<ResponseCode>
                ) {
                    val result = response.body().toString()
                    Log.d("API RESPONSE", result)
                    Log.d("종료 시간 ::", "${System.currentTimeMillis()}")
                }

                override fun onFailure(call: Call<ResponseCode>, t: Throwable) {
                    Log.e("ERROR_CALL", call.toString())
                    Log.e("ERROR", t.toString())
                }
            })
    }


    /**
     * [함수] GET 방식으로 BASE64 데이터 전송 기능 함수
     */
    private fun getBase64() {
        retrofitConnection().getBase64(imageBase64 = "asdsd")
            .enqueue(object : Callback<ResponseCode> {
                // 응답을 받는 경우
                override fun onResponse(
                    call: Call<ResponseCode>,
                    response: Response<ResponseCode>
                ) {
                    // 200 / 300
                    if (response.isSuccessful) {
                        val result = response.body().toString()
                        Log.d("API RESPONSE", result)
                    } else {
                        Log.e("API FAIL", "실패입니다!!!!")
                    }
                }

                // 응답에 실패하는 경우
                override fun onFailure(call: Call<ResponseCode>, t: Throwable) {
                    Log.e("ERROR_CALL", call.toString())
                    Log.e("ERROR", t.toString())
                }
            })
    }
}


