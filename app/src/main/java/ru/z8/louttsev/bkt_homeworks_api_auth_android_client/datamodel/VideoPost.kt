package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import org.jetbrains.anko.doAsyncResult
import org.jetbrains.anko.uiThread
import java.net.URL
import java.util.UUID

class VideoPost(
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
    var videoUrl : String = ""
) : Post(id, Type.VIDEO, author, content, created, liked, likes, commented, comments, shared, shares, views)
{
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

    fun asyncUpdateVideoPreview(imageView : ImageView) {
        fun parseVideoUrl(url : String) = Regex("""v=""").split(url)[1]

        fun formPreviewUrl()
                = "https://img.youtube.com/vi/${parseVideoUrl(videoUrl)}/maxresdefault.jpg"

        doAsyncResult {
            val image = BitmapFactory
                .decodeStream(
                    URL(formPreviewUrl())
                        .openConnection()
                        .getInputStream())
            uiThread { imageView.setImageBitmap(image) }
        }
    }
}