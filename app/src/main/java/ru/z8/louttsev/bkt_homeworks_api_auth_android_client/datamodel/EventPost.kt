package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.UUID

class EventPost(
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
    var address : String = "",
    var location : Location? = null
) : Post(id, Type.EVENT, author, content, created, liked, likes, commented, comments, shared, shares, views)
{
    override fun complete(): String = address
    override fun open(context : Context) {
        val (latitude, longitude) = location!!
        context.startActivity(Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse("geo:$latitude,$longitude")
        })
    }

    data class Location(
        val latitude : Double,
        val longitude : Double
    )
}