package com.example.thesis_app.models

import retrofit2.http.GET

interface RandomWordApi {
    @GET("word")
    suspend fun getRandomWord(): List<String>
}
