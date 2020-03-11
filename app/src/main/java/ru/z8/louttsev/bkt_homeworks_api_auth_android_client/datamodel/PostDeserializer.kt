package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.util.UUID

class PostDeserializer : JsonDeserializer<Post> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Post {
        val data = json!!.asJsonObject
        val type = data.get("type").asString
        val id = UUID.fromString(data.get("id").asString)
        val post : Post? = when (type) {
            "TEXT" -> TextPost(id)
            "EVENT" -> EventPost(id)
            "VIDEO" -> VideoPost(id)
            "REPOST" -> Repost(id)
            "ADS" -> AdsPost(id)
            "IMAGE" -> ImagePost(id)
            else -> null // ignored
        }
        with(post!!) {
            author = data.get("author").asString
            isMy = data.get("isMy").asBoolean
            content = data.get("content").asString
            created = data.get("created").asLong
            isHide = data.get("isHide").asBoolean
            liked = data.get("liked").asBoolean
            likes = data.get("likes").asInt
            commented = data.get("commented").asBoolean
            comments = data.get("comments").asInt
            shared = data.get("shared").asBoolean
            shares = data.get("shares").asInt
            views = data.get("views").asInt

            when (post) {
                is EventPost -> {
                    post.address = data.get("address").asString
                    post.location = context!!
                        .deserialize(data.get("location"), EventPost.Location::class.java)
                }
                is VideoPost -> {
                    post.videoUrl = data.get("videoUrl").asString
                }
                is Repost -> {
                    post.source = UUID.fromString(data.get("source").asString)
                }
                is ImagePost -> {
                    post.imageUrl = data.get("imageUrl").asString
                }
                else -> {} // ignored
            }
        }
        return post
    }
}