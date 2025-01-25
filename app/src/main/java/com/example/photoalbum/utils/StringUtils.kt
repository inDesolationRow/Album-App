package com.example.photoalbum.utils

fun getThumbnailName(name: String, otherStr: String? = null): String {
    val other = otherStr?.let { "_$it" } ?: ""
    return name.split(".").dropLast(1).joinToString(".").plus(other).plus("_thumbnail.png")
}