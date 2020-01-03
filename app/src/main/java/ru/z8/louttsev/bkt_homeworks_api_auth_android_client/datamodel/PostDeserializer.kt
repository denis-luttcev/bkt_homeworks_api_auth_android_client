package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.Post.Type.*
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
            else -> null // ignored
        }
        with(post!!) {
            author = data.get("author").asString
            content = data.get("content").asString
            created = data.get("created").asLong
            liked = data.get("liked").asBoolean
            likes = data.get("likes").asInt
            commented = data.get("commented").asBoolean
            comments = data.get("comments").asInt
            shared = data.get("shared").asBoolean
            shares = data.get("shares").asInt
            views = data.get("views").asInt

            when (post.type) {
                TEXT -> {} // no special
                EVENT -> {
                    (post as EventPost).address = data.get("address").asString
                    post.location = context!!
                        .deserialize(data.get("location"), EventPost.Location::class.java)
                }
                VIDEO -> {
                    (post as VideoPost).url = data.get("url").asString
                }
                REPOST -> {
                    (post as Repost).source = UUID.fromString(data.get("source").asString)
                }
                ADS -> {} // ignored
            }
        }
        return post
    }
}