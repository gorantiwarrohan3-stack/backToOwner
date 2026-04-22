package com.wpi.backtoowner.domain.model

data class NewPost(
    val title: String,
    val description: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double,
    val type: PostType,
    val matchPercent: Int? = null,
)
