package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel

class Media(
    val filename: String = "",
    val imageUrl: String = "",
    val type: Type = Type.IMAGE
) {
    enum class Type {
        IMAGE
    }
}