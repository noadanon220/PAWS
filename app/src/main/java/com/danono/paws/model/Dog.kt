package com.danono.paws.model

data class Dog (
    val name: String,
    val birthDate: Long,
    val gender: Boolean,
    val weight: Double,
    val color: List<String>,
    val image: Int,
    val tags: List<String>,
    val breedName: String
) {

    class Builder(
        var name: String = "",
        var birthDate: Long = 0L,
        var gender: Boolean = false,
        var weight: Double = 0.0,
        var color: List<String> = mutableListOf(),
        var image: Int = 0,
        var tags: List<String> = mutableListOf(),
        var breedName: String = ""
    ) {
        fun name(name: String) = apply { this.name = name }
        fun birthDate(birthDate: Long) = apply { this.birthDate = birthDate }
        fun gender(gender: Boolean) = apply { this.gender = gender }
        fun weight(weight: Double) = apply { this.weight = weight }
        fun color(color: List<String>) = apply { this.color = color }
        fun image(image: Int) = apply { this.image = image }
        fun tags(tags: List<String>) = apply { this.tags = tags }
        fun breedName(breedName: String) = apply { this.breedName = breedName }

        fun build() = Dog(
            name,
            birthDate,
            gender,
            weight,
            color,
            image,
            tags,
            breedName
        )
    }
}