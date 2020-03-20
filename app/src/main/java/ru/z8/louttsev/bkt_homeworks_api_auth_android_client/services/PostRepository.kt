package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services

import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.AdsPost
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.Post
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.Repost
import java.util.UUID

interface PostRepository {
    fun isEmpty(): Boolean

    fun addPosts(posts: List<Post>)

    fun addAds(ads: List<AdsPost>)

    fun getListItemCount() : Int

    fun getPostsCount() : Int

    fun getAdsCount() : Int

    fun getItemByPosition(itemPosition: Int) : Post

    fun getPostById(id: UUID) : Post

    fun getPostByPosition(position: Int) : Post

    fun getPostPosition(itemPosition: Int) : Int

    fun findRepostSource(post: Repost): Post

    fun hidePost(post: Post)

    fun deletePost(post: Post)
}