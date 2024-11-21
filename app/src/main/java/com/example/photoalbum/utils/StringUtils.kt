package com.example.photoalbum.utils

fun getThumbnailName(name: String): String {
    return name.split(".").first().plus("_thumbnail.png")
}