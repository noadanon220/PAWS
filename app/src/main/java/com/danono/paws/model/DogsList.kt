package com.danono.paws.model

data class DogsList private constructor(
    val name: String, // Example: "<username> dogs"
    val allDogs: List<Dog>,
) {
    class Builder(
        var name: String = "",
        var allDogs: List<Dog> = mutableListOf()
    ) {
        fun name(name: String) = apply { this.name = name }
        fun addDog(dog: Dog) =
            apply { (this.allDogs as MutableList).add(dog) }//list is immutable, but i created is as mutable list, so i need to do casting to mutableList

        fun build() = DogsList(name, allDogs)
    }
}


