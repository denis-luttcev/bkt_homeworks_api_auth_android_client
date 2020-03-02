package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.post_card_layout.view.*
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.*
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.SchemaAPI

@KtorExperimentalAPI
class LayoutFiller(private val adapter: PostAdapter) {
    fun initPostCardLayout(itemView: View, post: Post, positionTag: Int) {
        with(itemView) {
            likeCb.isChecked = post.liked
            updateSocialCountView(post.likes, post.liked, likesCountTv)

            likeCb.tag = positionTag
            likeCb.setOnClickListener { view: View ->
                val thisPost: Post = repository.getPostByPosition(view.tag as Int)

                if ((view as CheckBox).isChecked) {
                    thisPost.like()
                    networkService.updateSocial(
                        thisPost.id,
                        SchemaAPI.SocialAction.LIKE,
                        SchemaAPI.Mode.POST
                    )
                } else {
                    thisPost.dislike()
                    networkService.updateSocial(
                        thisPost.id,
                        SchemaAPI.SocialAction.LIKE,
                        SchemaAPI.Mode.DELETE
                    )
                }

                updateSocialCountView(thisPost.likes, thisPost.liked, likesCountTv)
            }

            commentCb.isChecked = post.commented
            updateSocialCountView(post.comments, post.commented, commentsCountTv)

            commentCb.tag = positionTag
            commentCb.setOnClickListener { view: View ->
                val thisPost: Post = repository.getPostByPosition(view.tag as Int)

                if ((view as CheckBox).isChecked) {
                    //TODO: implement get comment text, its processing and display
                    thisPost.makeComment()
                    networkService.updateSocial(
                        thisPost.id,
                        SchemaAPI.SocialAction.COMMENT,
                        SchemaAPI.Mode.POST
                    )
                } else {
                    thisPost.removeComment()
                    networkService.updateSocial(
                        thisPost.id,
                        SchemaAPI.SocialAction.COMMENT,
                        SchemaAPI.Mode.DELETE
                    )
                }

                updateSocialCountView(thisPost.comments, thisPost.commented, commentsCountTv)
            }

            shareCb.isChecked = post.shared
            updateSocialCountView(post.shares, post.shared, sharesCountTv)

            shareCb.tag = positionTag
            shareCb.setOnClickListener { view: View ->
                val thisPost: Post = repository.getPostByPosition(view.tag as Int)

                if ((view as CheckBox).isChecked) {
                    thisPost.share(context)
                    networkService.updateSocial(
                        thisPost.id,
                        SchemaAPI.SocialAction.SHARE,
                        SchemaAPI.Mode.POST
                    )
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
                    networkService.updateSocial(
                        thisPost.id,
                        SchemaAPI.SocialAction.SHARE,
                        SchemaAPI.Mode.POST
                    )*/
                    //TODO: implement find and delete repost
                }

                updateSocialCountView(thisPost.shares, thisPost.shared, sharesCountTv)
            }

            hideBtn.tag = positionTag
            hideBtn.setOnClickListener { view: View ->
                val thisPost: Post = repository.getPostByPosition(view.tag as Int)

                repository.removePost(thisPost)

                adapter.notifyDataSetChanged()
            }
        }
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

                    updateImageView(post, previewIv)

                    playBtn.setOnClickListener {
                        post.open(context)
                    }
                }

                is Repost -> {
                    containerFl.visibility = View.VISIBLE

                    val repostView = LayoutInflater.from(context)
                        .inflate(R.layout.repost_layout, containerFl, false)

                    initPostView(repostView as ConstraintLayout, repository.findRepostSource(post))

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

                    updateImageView(post, previewIv)
                }

                else -> {} // ignored
            }
        }
    }

    fun updateSocialCountView(count: Int, isSelected: Boolean, textView: TextView) {
        textView.text = if (count > 0) count.toString() else ""
        textView.setTextColor(
            ContextCompat.getColor(
                textView.context,
                if (isSelected) R.color.colorSelected else R.color.colorSecondaryText
            )
        )
    }

    fun updateImageView(post: Post, imageView : ImageView) {
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