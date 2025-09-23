package com.example.thesis_app.models

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // --- Dictionary API ---
    private const val DICTIONARY_BASE_URL = "https://api.dictionaryapi.dev/"

    private fun getDictionaryInstance(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(DICTIONARY_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val dictionaryApi: DictionaryApi = getDictionaryInstance().create(DictionaryApi::class.java)


    // --- Random Word API ---
    private const val RANDOM_WORD_BASE_URL = "https://random-word-api.herokuapp.com/"

    private fun getRandomWordInstance(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(RANDOM_WORD_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val randomWordApi: RandomWordApi = getRandomWordInstance().create(RandomWordApi::class.java)
}
