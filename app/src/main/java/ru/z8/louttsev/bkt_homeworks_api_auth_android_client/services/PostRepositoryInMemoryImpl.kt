package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services

import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.AdsPost
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.Post
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.Repost
import java.util.UUID

private const val ADS_RATIO = 3 //  next ads after each 3 posts

class PostRepositoryInMemoryImpl : PostRepository {
    private val postList = mutableListOf<Post>()
    private val hiddenPosts = mutableListOf<Post>()
    private val adsList = mutableListOf<AdsPost>()
    private val adsIterator = adsList.circularIterator()

    override fun isEmpty(): Boolean = postList.isEmpty() && adsList.isEmpty()

    override fun addPosts(posts: List<Post>) {
        posts.reversed().forEach {
            if (it.isHide) {
                hiddenPosts.add(it)
            } else {
                postList.add(0, it)
            }
        }
    }

    override fun addAds(ads: List<AdsPost>) {
        adsList.addAll(ads)
    }

    override fun getListItemCount() = postList.size + postList.size / ADS_RATIO

    override fun getPostsCount() = postList.size + hiddenPosts.size

    override fun getAdsCount() = adsList.size

    override fun getItemByPosition(itemPosition: Int) =
        if (isPostPosition(itemPosition)) {
            postList[getPostPosition(itemPosition)]
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
        val post = postList.find { it.id == id }

        return if (post != null) {
            post
        } else {
            hiddenPosts.find { it.id == id }!!
        }
    }

    override fun getPostByPosition(position: Int): Post = postList[position]

    override fun findRepostSource(post: Repost): Post {
        val sourcePost = getPostById(post.source!!)

        return if (sourcePost !is Repost) sourcePost else findRepostSource(sourcePost)
    }

    override fun hidePost(post: Post) {
        post.hide()
        postList.remove(post)
        hiddenPosts.add(post)
    }

    override fun deletePost(post: Post) {
        postList.remove(post)
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