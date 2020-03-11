package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services

import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.AdsPost
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.Post
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.Repost
import java.util.UUID

private const val ADS_RATIO = 3 //  next ads after each 3 posts

class PostRepositoryInMemoryImpl : PostRepository {
    private val posts = mutableListOf<Post>()
    private val hiddenPosts = mutableListOf<Post>()
    private val ads = mutableListOf<AdsPost>()
    private val adsIterator = ads.circularIterator()

    override fun addPosts(posts: List<Post>) {
        posts.reversed().forEach {
            if (it.isHide) {
                hiddenPosts.add(it)
            } else {
                this.posts.add(0, it)
            }
        }
    }

    override fun addAds(ads: List<AdsPost>) {
        this.ads.addAll(ads)
    }

    override fun getListItemCount() = posts.size + posts.size / ADS_RATIO

    override fun getPostsCount() = posts.size + hiddenPosts.size

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

    private fun isPostPosition(itemPosition: Int) = (itemPosition + 1) % (ADS_RATIO + 1) != 0

    override fun getPostById(id: UUID) : Post {
        val post = posts.find { it.id == id }
        if (post != null) {
            return post
        } else {
            return hiddenPosts.find { it.id == id }!!
        }
    }

    override fun getPostByPosition(position: Int): Post = posts[position]

    override fun findRepostSource(post: Repost): Post {
        val sourcePost = getPostById(post.source!!)

        return if (sourcePost !is Repost) sourcePost else findRepostSource(sourcePost)
    }

    override fun hidePost(post: Post) {
        post.hide()
        posts.remove(post)
        hiddenPosts.add(post)
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