package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel

import android.content.Context
import java.util.UUID

class TextPost(
    id : UUID = UUID.randomUUID(),
    author : String = "",
    isMy: Boolean = false,
    content : String = "",
    created : Long = System.currentTimeMillis(), // in millis
    liked : Boolean = false,
    likes : Int = 0,
    commented : Boolean = false,
    comments : Int = 0,
    shared : Boolean = false,
    shares : Int = 0,
    views : Int = 0
) : Post(id, Type.TEXT, author, isMy, content, created, liked, likes, commented, comments, shared, shares, views)
{
    class RequestDto(model: TextPost) : Post.RequestDto(model)

    override fun toDto() = RequestDto(this)

    override fun complete(): String = ""
    override fun open(context : Context) {} // ignored
}