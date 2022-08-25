package com.example.bandeekotlin.model

/**
 * 결과값 반환
 */
data class Result(
    val code: Int,
    val isSuccess: Boolean,
    val message: String
)
