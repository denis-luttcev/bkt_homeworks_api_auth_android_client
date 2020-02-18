package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.post_card_layout.view.*
import kotlinx.android.synthetic.main.post_card_layout.view.hideBtn
import kotlinx.android.synthetic.main.post_card_layout.view.adsTv
import kotlinx.android.synthetic.main.post_card_layout.view.locationGrp
import kotlinx.android.synthetic.main.post_card_layout.view.previewIv
import kotlinx.android.synthetic.main.post_card_layout.view.playBtn
import kotlinx.android.synthetic.main.post_card_layout.view.containerFl
import kotlinx.android.synthetic.main.post_card_layout.view.authorTv
import kotlinx.android.synthetic.main.post_card_layout.view.createdTv
import kotlinx.android.synthetic.main.post_card_layout.view.contentTv
import kotlinx.android.synthetic.main.post_card_layout.view.viewsCountTv
import kotlinx.android.synthetic.main.post_card_layout.view.addressTv
import kotlinx.android.synthetic.main.post_card_layout.view.locationIv
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.*
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.SchemaAPI.*
import java.util.UUID

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

fun MutableList<AdsPost>.circularIterator() : CircularIterator {
    return CircularIterator(this)
}

@KtorExperimentalAPI
class PostAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val posts = mutableListOf<Post>()
    private val index = mutableMapOf<UUID, Post>()
    private val ads = mutableListOf<AdsPost>()
    private val adsIterator = ads.circularIterator()

    fun addPosts(posts: List<Post>) {
        this.posts.addAll(0, posts)
        index.putAll(posts.map { it.id to it }.toMap().toMutableMap())
    }

    fun addAds(ads: List<AdsPost>) {
        this.ads.addAll(ads)
    }

    fun getPostsCount() = posts.size

    fun getAdsCount() = ads.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.post_card_layout, parent, false)
        return PostViewHolder(view)
    }

    override fun getItemCount(): Int {
        return posts.size + posts.size / 3
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        var shiftedPosition = position
        val post = if ((position + 1) % 4 !=0) {
            shiftedPosition = position - (position + 1) / 4
            posts[shiftedPosition]
        } else {
            adsIterator.next()
        }

        with(holder.itemView) {
            likeCb.isChecked = post.liked
            updateSocialCountView(post.likes, post.liked, likesCountTv)
            likeCb.tag = shiftedPosition
            likeCb.setOnClickListener { view: View ->
                val thisPost: Post = posts[view.tag as Int]
                if ((view as CheckBox).isChecked) {
                    thisPost.like()
                    networkService.updateSocial(thisPost.id, SocialAction.LIKE, Mode.POST)
                } else {
                    thisPost.dislike()
                    networkService.updateSocial(thisPost.id, SocialAction.LIKE, Mode.DELETE)
                }
                updateSocialCountView(thisPost.likes, thisPost.liked, likesCountTv)
            }

            commentCb.isChecked = post.commented
            updateSocialCountView(post.comments, post.commented, commentsCountTv)
            commentCb.tag = shiftedPosition
            commentCb.setOnClickListener { view: View ->
                val thisPost: Post = posts[view.tag as Int]
                if ((view as CheckBox).isChecked) {
                    //TODO: implement get comment text, its processing and display
                    thisPost.makeComment()
                    networkService.updateSocial(thisPost.id, SocialAction.COMMENT, Mode.POST)
                } else {
                    thisPost.removeComment()
                    networkService.updateSocial(thisPost.id, SocialAction.COMMENT, Mode.DELETE)
                }
                updateSocialCountView(thisPost.comments, thisPost.commented, commentsCountTv)
            }

            shareCb.isChecked = post.shared
            updateSocialCountView(post.shares, post.shared, sharesCountTv)
            shareCb.tag = shiftedPosition
            shareCb.setOnClickListener { view: View ->
                val thisPost: Post = posts[view.tag as Int]
                if ((view as CheckBox).isChecked) {
                    thisPost.share(context)
                    networkService.updateSocial(thisPost.id, SocialAction.SHARE, Mode.POST)
                    // temporarily reposting
                    (context as MainActivity)
                        .prepareNewRepostBody(
                            Repost(author = "Netology", source = thisPost.id),
                            view,
                            sharesCountTv
                        )
                } else {
                    shareCb.isChecked = post.shared // temporarily enabled
                    /* temporarily disabled
                    thisPost.removeShare()
                    asyncUpdateSocial(thisPost.id, "share", Mode.DELETE)*/
                    //TODO: implement find and delete repost
                }
                updateSocialCountView(thisPost.shares, thisPost.shared, sharesCountTv)
            }

            hideBtn.tag = shiftedPosition
            hideBtn.setOnClickListener { view: View ->
                val thisPost: Post = posts[view.tag as Int]
                posts.remove(thisPost)
                notifyDataSetChanged()
            }
        }

        initPostView(holder.itemView as ConstraintLayout, post)
    }

    fun initPostView(view: ConstraintLayout, post: Post) {
        with(view) {
            this.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.colorSecondaryBackground
                )
            )
            findViewById<Group>(R.id.socialGrp).visibility = View.VISIBLE
            adsTv.visibility = View.GONE
            locationGrp.visibility = View.GONE
            previewIv.visibility = View.GONE
            playBtn.visibility = View.GONE
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
                    previewIv.visibility = View.VISIBLE
                    playBtn.visibility = View.VISIBLE
                    loadMedia(post, previewIv)
                    playBtn.setOnClickListener {
                        post.open(context)
                    }
                }
                is Repost -> {
                    containerFl.visibility = View.VISIBLE
                    val repostView = LayoutInflater.from(context)
                        .inflate(R.layout.repost_layout, containerFl, false)
                    initPostView(repostView as ConstraintLayout, findSource(post))
                    repostView.findViewById<Group>(R.id.socialGrp).visibility = View.GONE
                    repostView.adsTv.visibility = View.GONE
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
                    findViewById<Group>(R.id.socialGrp).visibility = View.GONE
                    contentTv.setOnClickListener {
                        post.open(context)
                    }
                }
                is ImagePost -> {
                    previewIv.visibility = View.VISIBLE
                    playBtn.visibility = View.GONE
                    loadMedia(post, previewIv)
                }
                else -> {} // ignored
            }
        }
    }

    fun findSource(post: Repost): Post {
        val sourcePost: Post = index.getValue(post.source!!)
        return if (sourcePost !is Repost) sourcePost else findSource(sourcePost)
    }

    fun getPostById(id: UUID) = index[id]

    fun updateSocialCountView(count: Int,
                              isSelected: Boolean,
                              textView: TextView) {

        textView.text = if (count > 0) count.toString() else ""
        textView.setTextColor(
            ContextCompat.getColor(
                textView.context,
                if (isSelected) R.color.colorSelected else R.color.colorSecondaryText
            )
        )
    }

    fun loadMedia(post: Post, imageView : ImageView) {
        val mediaUrl = when(post) {
            is ImagePost -> {
                post.imageUrl
            }
            is VideoPost -> {
                post.getImageUrl()
            }
            else -> { "" } // ignored
        }
        networkService.loadMedia(mediaUrl) {
            imageView.setImageBitmap(it)
        }
    }
}

class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)