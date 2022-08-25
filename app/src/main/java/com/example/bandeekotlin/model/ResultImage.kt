package com.example.bandeekotlin.model

import com.google.gson.annotations.SerializedName

data class ResultImage(
    @SerializedName("imageBase64")
    val imageBase64: String = "",

    @SerializedName("imageName")
    val imageName: String = "",

    @SerializedName("imageSize")
    val imageSize: Int = 0,

    @SerializedName("imageType")
    val imageType: String = "",
)
