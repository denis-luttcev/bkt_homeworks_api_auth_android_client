package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel

import android.content.Context
import java.util.UUID

class ImagePost(
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
    views : Int = 0,
    var imageUrl : String = ""
) : Post(id, Type.IMAGE, author, isMy, content, created, liked, likes, commented, comments, shared, shares, views)
{
    class RequestDto(model: ImagePost) : Post.RequestDto(model) {
        val imageUrl: String = model.imageUrl
    }

    override fun toDto() = RequestDto(this)

    override fun complete(): String = imageUrl
    override fun open(context: Context) {} // ignored
}