package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.UUID

class VideoPost(
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
    var videoUrl : String = ""
) : Post(id, Type.VIDEO, author, isMy, isHide, content, created, liked, likes, commented, comments, shared, shares, views)
{
    companion object {
        fun parseVideoUrl(url : String) = "https://img.youtube.com/vi/${Regex("""v=""").split(url)[1]}/maxresdefault.jpg"
    }

    class RequestDto(model: VideoPost) : Post.RequestDto(model) {
        val videoUrl: String = model.videoUrl
    }

    override fun toDto() = RequestDto(this)

    override fun complete(): String = videoUrl
    override fun open(context: Context) {
        context.startActivity(Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(videoUrl)
        })
    }

    fun getImageUrl() = parseVideoUrl(videoUrl)
}