package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import io.ktor.features.UnsupportedMediaTypeException
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.new_post_layout.*
import kotlinx.android.synthetic.main.post_card_layout.*
import kotlinx.android.synthetic.main.post_card_layout.view.*
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.*
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.AuthorizationException
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.LockedException
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.SchemaAPI.*

@KtorExperimentalAPI
class LayoutFiller(private val activity: MainActivity) {
    private val adapter = PostAdapter(this)

    fun notifyDataSetChanged() {
        adapter.notifyDataSetChanged()
    }

    fun initViews() {
        with(activity) {
            postListing.layoutManager = LinearLayoutManager(this)
            postListing.adapter = adapter

            swipeContainer.setOnRefreshListener {
                networkService.appendAds(repository.getAdsCount()) {
                    if (it != null) {
                        repository.addAds(it)

                    } else {
                        handleAuthorizationException()
                    }
                }

                updatePostsInAdapter(true)
            }

            prepareNewTextPostBody()

            val welcomeMessage = getString(R.string.welcome) + myself!!.username
            currentUser.text = welcomeMessage
        }
    }





    private fun updatePostsInAdapter(successfully: Boolean) {
        with(activity) {
            if (!successfully) {
                handleAuthorizationException()
                return
            }

            swipeContainer.isRefreshing = true
            networkService.appendPosts(repository.getPostsCount()) {
                if (it != null) {
                    repository.addPosts(it)
                    adapter.notifyDataSetChanged()

                    postListing.smoothScrollToPosition(0)

                    swipeContainer.isRefreshing = false

                } else {
                    handleAuthorizationException()
                }
            }
        }
    }

    fun initPostCardLayout(itemView: View, post: Post, positionTag: Int) {
        with(itemView) {
            likeCb.isChecked = post.liked
            updateSocialCountView(post.likes, post.liked, likesCountTv)

            likeCb.tag = positionTag
            likeCb.setOnClickListener { view: View ->
                val thisPost: Post = repository.getPostByPosition(view.tag as Int)

                if ((view as CheckBox).isChecked) {
                    networkService.updateSocial(thisPost.id, SocialAction.LIKE, Mode.POST) {
                        when (it) {
                            is AuthorizationException -> handleAuthorizationException()
                            is LockedException -> {
                                Toast.makeText(
                                    activity,
                                    R.string.locked_error_message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            null -> {
                                thisPost.like()
                                updateSocialCountView(thisPost.likes, thisPost.liked, likesCountTv)
                            }
                        }
                    }
                } else {
                    networkService.updateSocial(thisPost.id, SocialAction.LIKE, Mode.DELETE) {
                        when (it) {
                            is AuthorizationException -> handleAuthorizationException()
                            is LockedException -> {
                                Toast.makeText(
                                    activity,
                                    R.string.locked_error_message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            null -> {
                                thisPost.dislike()
                                updateSocialCountView(thisPost.likes, thisPost.liked, likesCountTv)
                            }
                        }
                    }
                }
            }

            commentCb.isChecked = post.commented
            updateSocialCountView(post.comments, post.commented, commentsCountTv)

            commentCb.tag = positionTag
            commentCb.setOnClickListener { view: View ->
                val thisPost: Post = repository.getPostByPosition(view.tag as Int)

                if ((view as CheckBox).isChecked) {

                    //TODO: implement get comment text, its processing and display

                    networkService.updateSocial(thisPost.id, SocialAction.COMMENT, Mode.POST) {
                        when (it) {
                            is AuthorizationException -> handleAuthorizationException()
                            is LockedException -> {
                                Toast.makeText(
                                    activity,
                                    R.string.locked_error_message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            null -> {
                                thisPost.makeComment()
                                updateSocialCountView(thisPost.comments, thisPost.commented, commentsCountTv)
                            }
                        }
                    }
                } else {
                    networkService.updateSocial(thisPost.id, SocialAction.COMMENT, Mode.DELETE) {
                        when (it) {
                            is AuthorizationException -> handleAuthorizationException()
                            is LockedException -> {
                                Toast.makeText(
                                    activity,
                                    R.string.locked_error_message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            null -> {
                                thisPost.removeComment()
                                updateSocialCountView(thisPost.comments, thisPost.commented, commentsCountTv)
                            }
                        }
                    }
                }
            }

            shareCb.isChecked = post.shared
            updateSocialCountView(post.shares, post.shared, sharesCountTv)

            shareCb.tag = positionTag
            shareCb.setOnClickListener { view: View ->
                val thisPost: Post = repository.getPostByPosition(view.tag as Int)

                if ((view as CheckBox).isChecked) {
                    networkService.updateSocial(thisPost.id, SocialAction.SHARE, Mode.POST) {
                        when (it) {
                            is AuthorizationException -> handleAuthorizationException()
                            is LockedException -> {
                                Toast.makeText(
                                    context,
                                    R.string.locked_error_message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            null -> {
                                thisPost.share(context)
                                // temporarily reposting
                                prepareNewRepostBody(
                                    Repost(author = myself!!.username, source = thisPost.id),
                                    view,
                                    sharesCountTv
                                )
                                updateSocialCountView(thisPost.shares, thisPost.shared, sharesCountTv)
                            }
                        }
                    }
                } else {
                    shareCb.isChecked = post.shared // temporarily enabled
                    /* temporarily disabled
                    networkService.updateSocial(thisPost.id, SocialAction.SHARE, Mode.POST) {
                        when (it) {
                            is AuthorizationException -> handleAuthorizationException()
                            is LockedException -> {
                                Toast.makeText(
                                    activity,
                                    R.string.locked_error_message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            null -> {
                                //TODO: implement find and delete repost
                                thisPost.removeShare()
                                updateSocialCountView(thisPost.shares, thisPost.shared, sharesCountTv)
                            }
                        }
                    }*/
                }
            }

            if (post.isMy) {
                hideBtn.visibility = View.GONE
                deleteBtn.visibility = View.VISIBLE
                editBtn.visibility = View.VISIBLE
            } else {
                hideBtn.visibility = View.VISIBLE
                deleteBtn.visibility = View.GONE
                editBtn.visibility = View.GONE
            }

            hideBtn.tag = positionTag
            hideBtn.setOnClickListener { view: View ->
                val thisPost: Post = repository.getPostByPosition(view.tag as Int)

                networkService.updateSocial(thisPost.id, SocialAction.HIDE, Mode.POST) {
                    when (it) {
                        is AuthorizationException -> handleAuthorizationException()
                        is LockedException -> {
                            Toast.makeText(
                                activity,
                                R.string.locked_error_message,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        null -> {
                            repository.hidePost(thisPost)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }

            deleteBtn.tag = positionTag
            deleteBtn.setOnClickListener { view: View ->
                val thisPost: Post = repository.getPostByPosition(view.tag as Int)

                networkService.deletePost(thisPost.id) {
                    if (!it) {
                        handleAuthorizationException()
                        return@deletePost
                    }

                    //TODO: handle case delete post which is source of repost...???

                    repository.deletePost(thisPost)
                    adapter.notifyDataSetChanged()
                }
            }

            editBtn.tag = positionTag
            editBtn.setOnClickListener { view: View ->
                val position = view.tag as Int
                val thisPost = repository.getPostByPosition(position)

                prepareEditPostBody(thisPost, position)
            }
        }
    }

    fun initPostView(itemView: View, post: Post) {
        with(itemView) {
            this.setBackgroundColor(
                ContextCompat.getColor(
                    activity,
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
                        post.open(activity)
                    }

                    locationIv.setOnClickListener {
                        post.open(activity)
                    }
                }

                is VideoPost -> {
                    previewIv.visibility = View.VISIBLE
                    playBtn.visibility = View.VISIBLE

                    updateImageView(post, previewIv)

                    playBtn.setOnClickListener {
                        post.open(activity)
                    }
                }

                is Repost -> {
                    containerFl.visibility = View.VISIBLE

                    val repostView = LayoutInflater.from(activity)
                        .inflate(R.layout.repost_layout, containerFl, false)

                    initPostView(repostView, repository.findRepostSource(post))

                    repostView.findViewById<Group>(R.id.socialGrp).visibility = View.GONE
                    repostView.adsTv.visibility = View.GONE

                    containerFl.addView(repostView)
                }

                is AdsPost -> {
                    this.setBackgroundColor(
                        ContextCompat.getColor(
                            activity,
                            R.color.colorAdsBackground
                        )
                    )

                    adsTv.visibility = View.VISIBLE
                    findViewById<Group>(R.id.socialGrp).visibility = View.GONE

                    contentTv.setOnClickListener {
                        post.open(activity)
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

    private fun updateSocialCountView(count: Int, isSelected: Boolean, textView: TextView) {
        textView.text = if (count > 0) count.toString() else ""
        textView.setTextColor(
            ContextCompat.getColor(
                activity,
                if (isSelected) R.color.colorSelected else R.color.colorSecondaryText
            )
        )
    }

    private fun updateImageView(post: Post, imageView : ImageView) {
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
            if (it == null) handleAuthorizationException()

            imageView.setImageBitmap(it)
        }
    }

    private fun prepareNewTextPostBody() {
        with(activity) {
            clearNewPostBody()

            textBtn.isChecked = true

            sendBtn.setOnClickListener {
                val content = newContentTv.text.toString()
                if (content.isEmpty() || content.isBlank()) {
                    Toast.makeText(
                        this,
                        R.string.new_post_hint,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val post = TextPost(author = myself!!.username, content = content)

                networkService.savePost(post, ::updatePostsInAdapter)

                clearNewPostBody()
            }

            cancelBtn.setOnClickListener {
                clearNewPostBody()
                textBtn.isChecked = true
            }

            textBtn.setOnClickListener {
                clearNewPostBody()
            }

            imageBtn.setOnClickListener {
                prepareNewImagePostBody()
            }

            eventBtn.setOnClickListener {
                prepareNewEventPostBody()
            }

            videoBtn.setOnClickListener {
                prepareNewVideoPostBody()
            }
        }
    }

    fun prepareNewImagePostBody() {
        with(activity) {
            clearNewPostBody()

            newPreviewIv.visibility = View.VISIBLE

            newGalleryBtn.visibility = View.VISIBLE
            newGalleryBtn.setOnClickListener {
                getImageContent()
            }

            newCameraBtn.visibility = View.VISIBLE
            newCameraBtn.setOnClickListener {
                getCameraContent()
            }

            sendBtn.setOnClickListener {
                val content = newContentTv.text.toString()
                if (content.isEmpty() || content.isBlank()) {
                    Toast.makeText(
                        this,
                        R.string.new_post_hint,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val imageUrl = newPreviewIv.tag.toString()
                if (imageUrl.isEmpty() || imageUrl.isBlank()) {
                    Toast.makeText(
                        this,
                        R.string.image_uri_error_message,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val post =
                    ImagePost(author = myself!!.username, content = content, imageUrl = imageUrl)

                networkService.savePost(post, ::updatePostsInAdapter)

                prepareNewTextPostBody()
            }
        }
    }

    private fun prepareNewEventPostBody() {
        with(activity) {
            clearNewPostBody()

            newLocationGrp.visibility = View.VISIBLE

            sendBtn.setOnClickListener {
                val content = newContentTv.text.toString()
                if (content.isEmpty() || content.isBlank()) {
                    Toast.makeText(
                        this,
                        R.string.new_post_hint,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val address = newAddressEt.text.toString()
                if (address.isEmpty() || address.isBlank()) {
                    Toast.makeText(
                        this,
                        R.string.event_address_error_message,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val post =
                    EventPost(author = myself!!.username, content = content, address = address)

                networkService.savePost(post, ::updatePostsInAdapter)

                prepareNewTextPostBody()
            }
        }
    }

    private fun prepareNewVideoPostBody() {
        with(activity) {
            clearNewPostBody()

            newVideoUrlEt.visibility = View.VISIBLE
            newVideoUrlEt.setOnFocusChangeListener(::handleVideoUrl)

            sendBtn.setOnClickListener {
                val content = newContentTv.text.toString()
                if (content.isEmpty() || content.isBlank()) {
                    Toast.makeText(
                        this,
                        R.string.new_post_hint,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val inputUrl = newVideoUrlEt.text.toString()
                if (isNotYouTubeUrl(inputUrl)) {
                    Toast.makeText(
                        this,
                        R.string.new_post_hint,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val post =
                    VideoPost(author = myself!!.username, content = content, videoUrl = inputUrl)

                networkService.savePost(post, ::updatePostsInAdapter)

                prepareNewTextPostBody()
            }
        }
    }

    private fun handleVideoUrl(view: View, focused: Boolean) {
        with(activity) {
            if (!focused) {
                if (videoBtn.isChecked) {

                    if (!updateVideoPreview()) return

                    newPreviewIv.visibility = View.VISIBLE
                    newPlayBtn.visibility = View.VISIBLE
                    newVideoUrlEt.visibility = View.GONE

                } else {
                    newVideoUrlEt.text.clear()
                    newVideoUrlEt.visibility = View.GONE
                }
            }
        }
    }

    private fun updateVideoPreview(): Boolean {
        val inputUrl = activity.newVideoUrlEt.text.toString()

        if (isNotYouTubeUrl(inputUrl)) {
            Toast.makeText(
                activity,
                R.string.video_url_error_message,
                Toast.LENGTH_SHORT
            ).show()
            activity.newVideoUrlEt.text.clear()
            return false
        }

        networkService.loadMedia(parseVideoUrl(inputUrl)) {
            if (it == null) handleAuthorizationException()

            activity.newPreviewIv.setImageBitmap(it)
        }

        return true
    }

    private fun isNotYouTubeUrl(inputUrl: String)
            = !inputUrl.contains("https://www.youtube.com/watch?v=")

    private fun prepareNewRepostBody(post: Post, view: CheckBox?, countView: TextView?) {
        with(activity) {

            typeGrp.visibility = View.GONE

            when (post) {
                is EventPost -> {
                    newLocationGrp.visibility = View.VISIBLE
                    newAddressEt.setText(post.address)
                    newAddressEt.setOnClickListener {
                        post.open(this)
                    }
                    newLocationIv.setOnClickListener {
                        post.open(this)
                    }
                }

                is VideoPost -> {
                    newPreviewIv.visibility = View.VISIBLE

                    newPlayBtn.visibility = View.VISIBLE
                    newPlayBtn.setOnClickListener {
                        post.open(this)
                    }

                    updateImageView(post, newPreviewIv)
                }

                is Repost -> {
                    newContainerFl.visibility = View.VISIBLE

                    val repostView = LayoutInflater.from(this)
                        .inflate(R.layout.repost_layout, containerFl, false)
                    initPostView(repostView, repository.findRepostSource(post))

                    repostView.findViewById<Group>(R.id.socialGrp).visibility = View.GONE
                    repostView.adsTv.visibility = View.GONE

                    newContainerFl.addView(repostView)
                }

                is ImagePost -> {
                    newPreviewIv.visibility = View.VISIBLE
                    newPlayBtn.visibility = View.GONE

                    updateImageView(post, previewIv)
                }

                else -> {} // ignored
            }

            sendBtn.setOnClickListener {
                val content = newContentTv.text.toString()
                if (content.isEmpty() || content.isBlank()) {
                    post.content = getString(R.string.repost_text_default) // default content
                } else {
                    post.content = content
                }

                networkService.savePost(post, ::updatePostsInAdapter)

                prepareNewTextPostBody()
            }

            cancelBtn.setOnClickListener {
                if (post is Repost) {
                    val sharedPost = repository.getPostById(post.source!!)

                    networkService.updateSocial(post.source!!, SocialAction.SHARE, Mode.DELETE) {
                        when (it) {
                            is AuthorizationException -> handleAuthorizationException()
                            is LockedException -> {
                                Toast.makeText(
                                    this,
                                    R.string.locked_error_message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            null -> {
                                sharedPost.removeShare()
                                view!!.isChecked = false

                                updateSocialCountView(sharedPost.shares, false, countView!!)
                            }
                        }
                    }
                }

                prepareNewTextPostBody()
            }
        }
    }

    private fun prepareEditPostBody(post: Post, position: Int) {
        with(activity) {
            clearNewPostBody()
            typeGrp.visibility = View.GONE

            newContentTv.setText(post.content)

            when (post) {
                is TextPost -> {
                    sendBtn.setOnClickListener {
                        val content = newContentTv.text.toString()
                        if (content.isEmpty() || content.isBlank()) {
                            Toast.makeText(
                                this,
                                R.string.new_post_hint,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        post.content = content
                        updatePostData(post, position)
                    }
                }

                is EventPost -> {
                    newLocationGrp.visibility = View.VISIBLE

                    newAddressEt.setText(post.address)

                    sendBtn.setOnClickListener {
                        val content = newContentTv.text.toString()
                        if (content.isEmpty() || content.isBlank()) {
                            Toast.makeText(
                                this,
                                R.string.new_post_hint,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        val address = newAddressEt.text.toString()
                        if (address.isEmpty() || address.isBlank()) {
                            Toast.makeText(
                                this,
                                R.string.event_address_error_message,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        post.content = content
                        post.address = address
                        updatePostData(post, position)
                    }
                }

                is VideoPost -> {
                    newPreviewIv.visibility = View.VISIBLE
                    newPlayBtn.visibility = View.VISIBLE
                    newVideoUrlEt.visibility = View.VISIBLE

                    updateImageView(post, newPreviewIv)
                    newVideoUrlEt.setText(post.videoUrl)

                    newVideoUrlEt.setOnFocusChangeListener { _, _ -> Unit
                        if (!updateVideoPreview()) return@setOnFocusChangeListener
                    }

                    sendBtn.setOnClickListener {
                        val content = newContentTv.text.toString()
                        if (content.isEmpty() || content.isBlank()) {
                            Toast.makeText(
                                this,
                                R.string.new_post_hint,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        val inputUrl = newVideoUrlEt.text.toString()
                        if (isNotYouTubeUrl(inputUrl)) {
                            Toast.makeText(
                                this,
                                R.string.new_post_hint,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        post.content = content
                        post.videoUrl = inputUrl
                        updatePostData(post, position)
                    }
                }

                is Repost -> {
                    newContainerFl.visibility = View.VISIBLE

                    val repostView = LayoutInflater.from(this)
                        .inflate(R.layout.repost_layout, containerFl, false)
                    initPostView(repostView, repository.findRepostSource(post))

                    repostView.findViewById<Group>(R.id.socialGrp).visibility = View.GONE
                    repostView.adsTv.visibility = View.GONE

                    newContainerFl.addView(repostView)

                    sendBtn.setOnClickListener {
                        val content = newContentTv.text.toString()
                        if (content.isEmpty() || content.isBlank()) {
                            post.content = getString(R.string.repost_text_default) // default content
                        } else {
                            post.content = content
                        }

                        post.content = content
                        updatePostData(post, position)
                    }
                }

                is ImagePost -> {
                    newPreviewIv.visibility = View.VISIBLE
                    newPlayBtn.visibility = View.GONE

                    updateImageView(post, newPreviewIv)
                    newPreviewIv.tag = post.imageUrl

                    newPreviewIv.setColorFilter(Color.argb(150, 255, 255, 255))

                    newGalleryBtn.visibility = View.VISIBLE
                    newGalleryBtn.setOnClickListener {
                        getImageContent()
                    }

                    newCameraBtn.visibility = View.VISIBLE
                    newCameraBtn.setOnClickListener {
                        getCameraContent()
                    }

                    sendBtn.setOnClickListener {
                        val content = newContentTv.text.toString()
                        if (content.isEmpty() || content.isBlank()) {
                            Toast.makeText(
                                this,
                                R.string.new_post_hint,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        val imageUrl = newPreviewIv.tag.toString()
                        if (imageUrl.isEmpty() || imageUrl.isBlank()) {
                            Toast.makeText(
                                this,
                                R.string.image_uri_error_message,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        post.content = content
                        post.imageUrl = imageUrl
                        updatePostData(post, position)
                    }
                }

                else -> {} // ignored
            }

            cancelBtn.setOnClickListener {
                prepareNewTextPostBody()
            }
        }
    }

    private fun clearNewPostBody() {
        with(activity) {
            newContentTv.text.clear()
            newLocationGrp.visibility = View.GONE
            newPreviewIv.setImageURI(null)
            newPreviewIv.tag = null
            newPreviewIv.visibility = View.GONE
            newVideoUrlEt.text.clear()
            newVideoUrlEt.visibility = View.GONE
            newGalleryBtn.visibility = View.GONE
            newCameraBtn.visibility = View.GONE
            newPlayBtn.visibility = View.GONE
            newContainerFl.visibility = View.GONE
            newContainerFl.removeAllViews()
            typeGrp.visibility = View.VISIBLE
            newAddressEt.text.clear()
        }
    }

    private fun updatePostData(post: Post, position: Int) {
        networkService.updatePost(post) {
            if (!it) {
                handleAuthorizationException()
                return@updatePost
            }

            adapter.notifyDataSetChanged()
            activity.postListing.smoothScrollToPosition(position)
            prepareNewTextPostBody()
        }
    }

    fun handleGalleryImage(imageUri: Uri) {
        with(activity) {
            newPreviewIv.setImageURI(imageUri)
            newPreviewIv.clearColorFilter()

            newGalleryBtn.visibility = View.GONE
            newCameraBtn.visibility = View.GONE

            networkService.saveMedia(imageUri, this) { url: String?, cause: Throwable? ->
                if (url != null) {
                    newPreviewIv.tag = url
                } else {
                    when (cause) {
                        is AuthorizationException -> handleAuthorizationException()
                        is UnsupportedMediaTypeException -> {
                            Toast.makeText(
                                this,
                                R.string.unsupported_media_error_message,
                                Toast.LENGTH_SHORT
                            ).show()

                            prepareNewImagePostBody()
                        }
                        else -> {
                            Toast.makeText(
                                this,
                                R.string.load_error_message,
                                Toast.LENGTH_SHORT
                            ).show()

                            prepareNewImagePostBody()
                        }
                    }
                }
            }
        }
    }

    fun handleCameraImage() {
        //TODO: implement camera image handle
    }
}