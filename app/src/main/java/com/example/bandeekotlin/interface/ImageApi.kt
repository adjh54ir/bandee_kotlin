package com.example.bandeekotlin.`interface`

import com.example.bandeekotlin.model.PostImage
import com.example.bandeekotlin.model.ResponseCode
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.*


interface ImageApi {

    /**
     * GET - 방식을 통해 서버로 base64를 전송한다.
     */
    @GET("/getBase64")
    fun getBase64(
        @Query("imageBase64") imageBase64: String,
    ): Call<ResponseCode>

    /**
     * POST - 서버로 base64를 전송한다.
     */
    @POST("/getBase64")
    fun postBase64(@Body post: PostImage): Call<ResponseCode>


    /**
     * POST - 서버로 Multi-part Image 데이터를 전송한다.
     */
    @Multipart
    @POST("/postImage")
    fun postImage(
//        @Part image: MultipartBody.Part,        // 단건 전송
        @Part image: List<MultipartBody.Part>,        // 단건 전송
    ): Call<ResponseCode>
}