package com.example.bandeekotlin.`interface`

import com.example.bandeekotlin.model.PostImage
import com.example.bandeekotlin.model.ResponseCode
import com.example.bandeekotlin.model.ResultImage
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.*


interface ImageApi {

    @POST("/")
    fun getRoot(): Call<JSONObject>

    // GET 방식을 통해서 base64를 전송한다.
    @GET("/getBase64")
    fun getBase64(
        @Query("imageBase64") imageBase64: String,
    ): Call<ResponseCode>

    @POST("/getBase64")
    open fun postBase64(@Body post: PostImage): Call<ResponseCode>


    @Multipart
    @POST("/postImage")
    fun postImage(
//        @Part image: MultipartBody.Part,        // 단건 전송
        @Part image: List<MultipartBody.Part>,        // 단건 전송
    ): Call<ResponseCode>



//    @GET("/getFlask")
//    fun getImage(
//        @Query("imageBase64") imageBase64: String,
//        @Query("imageName") imageName: String,
//        @Query("imageSize") imageSize: String,
//        @Query("imageType") imageType: String,
////        @Part imageFile: MultipartBody.Part
//    ): Call<ResultImage>
//
//    @GET("/getObj/{imageBase64}")
//    fun getObj(@Path("imageBase64") imageBase64: String): Call<JSONObject>
//
//    @POST("/mongo")
//    fun postImage(
//        @Field("imageBase64") imageBase64: String,
//        @Field("imageName") imageName: String,
//        @Field("imageSize") imageSize: String,
//        @Field("imageType") imageType: String,
//    ): Call<ResultImage>

}