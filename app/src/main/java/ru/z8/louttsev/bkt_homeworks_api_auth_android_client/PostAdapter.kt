package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.withCharset
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.post_card_layout.view.*
import kotlinx.coroutines.*
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.*
import java.util.UUID

fun List<AdsPost>.circularIterator() : CircularIterator {
    return CircularIterator(this)
}

class CircularIterator(private val list: List<AdsPost>) : Iterator<AdsPost> {
    private var index: Int = -1

    override fun hasNext(): Boolean = index < list.size - 1

    override fun next(): AdsPost {
        if (hasNext()) {
            index++
        } else {
            index = 0

        }
        return list[index]
    }
}

@KtorExperimentalAPI
class PostAdapter(
    private val list : MutableList<Post>,
    ads: MutableList<AdsPost>,
    private val listingRv: RecyclerView
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), CoroutineScope by MainScope() {

    private val index: MutableMap<UUID, Post> = list.map { it.id to it }.toMap().toMutableMap()

    private val adsIterator: Iterator<AdsPost> = ads.circularIterator()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_card_layout, parent, false)
        return PostViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size + list.size / 3
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val post = if ((position + 1) % 4 !=0) {
            list[position - (position + 1) / 4]
        } else {
            adsIterator.next()
        }

        with(holder.itemView) {
            likeCb.isChecked = post.liked
            updateSocialCountView(post.likes, post.liked, likesCountTv)
            likeCb.tag = position
            likeCb.setOnClickListener { view: View ->
                val thisPost: Post = list[view.tag as Int]
                if ((view as CheckBox).isChecked) {
                    thisPost.like()
                    asyncUpdateSocial(thisPost.id, "like", Mode.POST)
                } else {
                    thisPost.dislike()
                    asyncUpdateSocial(thisPost.id, "like", Mode.DELETE)
                }
                updateSocialCountView(thisPost.likes, thisPost.liked, likesCountTv)
            }

            commentCb.isChecked = post.commented
            updateSocialCountView(post.comments, post.commented, commentsCountTv)
            commentCb.tag = position
            commentCb.setOnClickListener { view: View ->
                val thisPost: Post = list[view.tag as Int]
                if ((view as CheckBox).isChecked) {
                    //TODO: implement get comment text, its processing and display
                    thisPost.makeComment()
                    asyncUpdateSocial(thisPost.id, "comment", Mode.POST)
                } else {
                    thisPost.removeComment()
                    asyncUpdateSocial(thisPost.id, "comment", Mode.DELETE)
                }
                updateSocialCountView(thisPost.comments, thisPost.commented, commentsCountTv)
            }

            shareCb.isChecked = post.shared
            updateSocialCountView(post.shares, post.shared, sharesCountTv)
            shareCb.tag = position
            shareCb.setOnClickListener { view: View ->
                val thisPost: Post = list[view.tag as Int]
                if ((view as CheckBox).isChecked) {
                    thisPost.share(context)
                    asyncUpdateSocial(thisPost.id, "share", Mode.POST)
                    // temporarily reposting
                    val newPost =
                        Repost(author = "Netology", content = "Reposted", source = thisPost.id)
                    savePost(newPost)
                } else {
                    shareCb.isChecked = post.shared // temporarily enabled
                    /* temporarily disabled
                    thisPost.removeShare()
                    asyncUpdateSocial(thisPost.id, "share", Mode.DELETE)*/
                    //TODO: implement find and delete repost
                }
                updateSocialCountView(thisPost.shares, thisPost.shared, sharesCountTv)
            }

            hideBtn.tag = position
            hideBtn.setOnClickListener { view: View ->
                val thisPost: Post = list[view.tag as Int]
                list.remove(thisPost)
                notifyDataSetChanged()
            }
        }

        initPostView(holder.itemView as ConstraintLayout, post)
    }

    private fun initPostView(view: ConstraintLayout, post: Post) {
        with(view) {
            this.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.colorSecondaryBackground
                )
            )
            socialGrp.visibility = View.VISIBLE
            adsTv.visibility = View.GONE
            locationGrp.visibility = View.GONE
            videoGrp.visibility = View.GONE
            containerFl.visibility = View.GONE
            containerFl.removeAllViews()

            authorTv.text = post.author
            createdTv.text = post.age
            contentTv.text = post.content
            viewsCountTv.text = if (post.views > 0) post.views.toString() else ""

            when (post) {
                is EventPost -> {
                    locationGrp.visibility = View.VISIBLE
                    addressTv.text = post.address
                    addressTv.setOnClickListener {
                        post.open(context)
                    }
                    locationIv.setOnClickListener {
                        post.open(context)
                    }
                }
                is VideoPost -> {
                    videoGrp.visibility = View.VISIBLE
                    post.asyncUpdateVideoPreview(previewIv)
                    playBtn.setOnClickListener {
                        post.open(context)
                    }
                }
                is Repost -> {
                    containerFl.visibility = View.VISIBLE
                    val repostView = LayoutInflater.from(context)
                        .inflate(R.layout.repost_layout, containerFl, false)
                    initPostView(repostView as ConstraintLayout, findSource(post))
                    repostView.socialGrp.visibility = View.GONE
                    containerFl.addView(repostView)
                }
                is AdsPost -> {
                    this.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.colorAdsBackground
                        )
                    )
                    adsTv.visibility = View.VISIBLE
                    socialGrp.visibility = View.GONE
                    contentTv.setOnClickListener {
                        post.open(context)
                    }
                }
                else -> {
                } // ignored
            }
        }
    }

    private fun findSource(post: Repost): Post {
        val sourcePost: Post = index.getValue(post.source!!)
        return if (sourcePost !is Repost) sourcePost else findSource(sourcePost)
    }

    private fun View.updateSocialCountView(
        count: Int,
        isSelected: Boolean,
        textView: TextView
    ) {
        textView.text = if (count > 0) count.toString() else ""
        textView.setTextColor(
            ContextCompat.getColor(
                context,
                if (isSelected) R.color.colorSelected else R.color.colorSecondaryText
            )
        )
    }

    fun diluteWithAds(adsList: MutableList<AdsPost>): PostAdapter {
        val postsListSize = list.size
        val adsListSize = adsList.size
        val ratio: Int = postsListSize / adsListSize
        val adsLimit: Int = if (ratio >= 3) adsListSize else postsListSize / 3

        adsList.forEachIndexed { index, adsPost ->
            if (index < adsLimit) {
                list.add((index + 1) * 3 + index, adsPost)
            } else return@forEachIndexed
        }

        return this
    }

    private fun asyncUpdateSocial(id: UUID, attribute: String, mode: Mode) = launch {
        withContext(Dispatchers.IO) {
            val client = HttpClient()
            val url =
                "https://api-auth-server-luttcev.herokuapp.com/api/v1/posts/${id}/${attribute}"
            when (mode) {
                Mode.POST -> client.post<String>(url)
                Mode.DELETE -> client.delete<String>(url)
            }
            client.close()
        }
    }

    fun savePost(post: Post) = launch(Dispatchers.Main) {
        withContext(Dispatchers.IO) {
            val client = HttpClient {
                install(JsonFeature) {
                    acceptContentTypes = listOf(
                        ContentType.Application.Json
                    )
                }
            }
            post.id = client.post {
                url("https://api-auth-server-luttcev.herokuapp.com/api/v1/posts")
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                body = Gson().toJsonTree(Post.fromModel(post))
            }
            client.close()
        }

        updateData()
    }

    fun updateData() = launch(Dispatchers.Main) {
        lateinit var newPosts : List<Post>

        withContext(Dispatchers.IO) {
            val client = HttpClient {
                install(JsonFeature) {
                    acceptContentTypes = listOf(
                        ContentType.Application.Json
                    )
                    serializer = GsonSerializer {
                        registerTypeAdapter(Post::class.java, PostDeserializer())
                    }
                }
            }
            val url = "https://api-auth-server-luttcev.herokuapp.com/api/v1/posts/${list.size}"
            newPosts = client.get<List<Post>>(url).toList()

            client.close()
        }

        list.addAll(0, newPosts)
        index.putAll(newPosts.map { it.id to it}.toMap())
        notifyItemRangeInserted(0, newPosts.size)

        listingRv.smoothScrollToPosition(0)
    }
}

class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

enum class Mode {
    POST, DELETE
}