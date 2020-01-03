package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.UUID

class AdsPost(
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
    var url : String = ""
) : Post(id, Type.ADS, author, content, created, liked, likes, commented, comments, shared, shares, views)
{
    override fun complete(): String = url
    override fun open(context: Context) {
        context.startActivity(Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(url)
        })
    }
}