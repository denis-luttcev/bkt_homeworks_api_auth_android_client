package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services

import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.AdsPost
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.Post
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.Repost
import java.util.UUID

private const val ADS_RATIO = 3 //  next ads after each 3 posts

class PostRepositoryInMemoryImpl : PostRepository {
    private val posts = mutableListOf<Post>()
    private val index = mutableMapOf<UUID, Post>()
    private val ads = mutableListOf<AdsPost>()
    private val adsIterator = ads.circularIterator()

    override fun addPosts(posts: List<Post>) {
        this.posts.addAll(0, posts)
        index.putAll(posts.map { it.id to it }.toMap().toMutableMap())
    }

    override fun addAds(ads: List<AdsPost>) {
        this.ads.addAll(ads)
    }

    override fun getListItemCount() = posts.size + posts.size * ADS_RATIO

    override fun getPostsCount() = posts.size

    override fun getAdsCount() = ads.size

    override fun getItemByPosition(itemPosition: Int) =
        if (isPostPosition(itemPosition)) {
            posts[getPostPosition(itemPosition)]
        } else {
            adsIterator.next()
        }

    override fun getPostPosition(itemPosition: Int) =
        if (isPostPosition(itemPosition)) {
            itemPosition - (itemPosition + 1) / (ADS_RATIO + 1)
        } else {
            -1
        }

    private fun isPostPosition(itemPosition: Int) :Boolean {
        val a = (itemPosition + 1) % (ADS_RATIO + 1)
        val b = (itemPosition + 1) % 4

        return (itemPosition + 1) % (ADS_RATIO + 1) != 0
    }

    override fun getPostById(id: UUID) = index[id]!!

    override fun getPostByPosition(position: Int): Post = posts[position]

    override fun findRepostSource(post: Repost): Post {
        val sourcePost = index.getValue(post.source!!)

        return if (sourcePost !is Repost) sourcePost else findRepostSource(sourcePost)
    }

    override fun removePost(post: Post) {
        posts.remove(post)
    }
}


fun MutableList<AdsPost>.circularIterator() : CircularIterator {
    return CircularIterator(this)
}

class CircularIterator(private val ads: MutableList<AdsPost>) : Iterator<AdsPost> {
    private var index: Int = -1

    override fun hasNext(): Boolean = index < ads.size - 1

    override fun next(): AdsPost {
        if (hasNext()) {
            index++
        } else {
            index = 0
        }

        return ads[index]
    }
}