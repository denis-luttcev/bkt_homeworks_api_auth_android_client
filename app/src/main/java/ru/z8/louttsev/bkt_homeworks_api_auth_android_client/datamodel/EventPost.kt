package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.UUID

class EventPost(
    id : UUID = UUID.randomUUID(),
    author : String = "",
    isMy: Boolean = false,
    isHide: Boolean = false,
    content : String = "",
    created : Long = System.currentTimeMillis(), // in millis
    liked : Boolean = false,
    likes : Int = 0,
    commented : Boolean = false,
    comments : Int = 0,
    shared : Boolean = false,
    shares : Int = 0,
    views : Int = 0,
    var address : String = "",
    var location : Location = Location()
) : Post(id, Type.EVENT, author, isMy, isHide, content, created, liked, likes, commented, comments, shared, shares, views)
{
    class RequestDto(model: EventPost) : Post.RequestDto(model) {
        val address: String = model.address
        val location: Location = model.location
    }

    override fun toDto() = RequestDto(this)

    override fun complete(): String = address
    override fun open(context : Context) {
        val (latitude, longitude) = location
        val query = "geo:$latitude,$longitude?q=$address"
        context.startActivity(Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(query)
        })
    }

    data class Location(
        val latitude : Double = 0.0,
        val longitude : Double = 0.0
    )
}