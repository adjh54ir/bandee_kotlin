package com.example.bandeekotlin.`interface`

import com.example.bandee.model.ResultImage
import org.json.JSONObject
import retrofit2.Call
import retrofit2.http.*

interface ImageApi {

    @POST("/")
    fun getRoot(): Call<JSONObject>

    // GET 방식을 통해서 base64를 전송한다.
    @GET("/getBase64")
    fun getBase64(
        @Query("imageBase64") imageBase64: String,
    ): Call<JSONObject>


    @GET("/getFlask")
    fun getImage(
        @Query("imageBase64") imageBase64: String,
        @Query("imageName") imageName: String,
        @Query("imageSize") imageSize: String,
        @Query("imageType") imageType: String,
//        @Part imageFile: MultipartBody.Part
    ): Call<ResultImage>

    @GET("/getObj/{imageBase64}")
    fun getObj(@Path("imageBase64") imageBase64: String): Call<JSONObject>

    @POST("/mongo")
    fun postImage(
        @Field("imageBase64") imageBase64: String,
        @Field("imageName") imageName: String,
        @Field("imageSize") imageSize: String,
        @Field("imageType") imageType: String,
    ): Call<ResultImage>

}