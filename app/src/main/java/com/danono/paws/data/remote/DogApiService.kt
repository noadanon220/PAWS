package com.danono.paws.data.remote

import retrofit2.http.GET
import com.danono.paws.model.DogBreed

interface DogApiService {

    // Returns a list of dog breeds with basic info (including breed name)
    @GET("v1/breeds")
    suspend fun getAllBreeds(): List<DogBreed>
}
