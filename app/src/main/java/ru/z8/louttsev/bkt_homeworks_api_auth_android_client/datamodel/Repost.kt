package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel

import android.content.Context
import java.util.UUID

class Repost(
    id : UUID = UUID.randomUUID(),
    author : String = "",
    content : String = "",
    created : Long = System.currentTimeMillis(), // in millis
    liked : Boolean = false,
    likes : Int = 0,
    commented : Boolean = false,
    comments : Int = 0,
    shared : Boolean = false,
    shares : Int = 0,
    views : Int = 0,
    var source : UUID? = null
) : Post(id, Type.REPOST, author, content, created, liked, likes, commented, comments, shared, shares, views)
{
    class RequestDto(model: Repost) : Post.RequestDto(model) {
        val source: UUID? = model.source
    }

    override fun toDto() = RequestDto(this)

    override fun complete(): String = "This is repost of " + source.toString()
    override fun open(context: Context) {} // ignored
}