package com.example.bandeekotlin.model

import com.google.gson.annotations.SerializedName

data class PostImage(
    @SerializedName("imageBase64")
    private val imageBase64: String = "",
)
