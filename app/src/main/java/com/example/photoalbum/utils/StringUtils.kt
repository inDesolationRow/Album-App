package com.example.photoalbum.utils

fun getThumbnailName(name: String, otherStr: String? = null): String {
    val other = otherStr?.let { "_$it" } ?: ""
    return name.split(".").dropLast(1).joinToString(".").plus(other).plus("_thumbnail.png")
}

fun getLastPath(address: String): String {
    val parts = address.split("/").filter { it.isNotBlank() }
    return parts.last()
}

fun getPaths(address: String): List<String> {
    val parts = address.split("/").filter { it.isNotBlank() }
    val result = mutableListOf<String>()

    // 累积字符串
    var cumulativePath = ""
    for (part in parts) {
        // 如果是第一次则直接赋值，否则在之前的基础上拼接
        cumulativePath = if (cumulativePath.isEmpty()) {
            part
        } else {
            "$cumulativePath/$part"
        }
        result.add(cumulativePath)
    }
    return result
}