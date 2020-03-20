package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.View.*
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.new_post_layout.*
import kotlinx.android.synthetic.main.post_card_layout.*
import kotlinx.android.synthetic.main.post_card_layout.view.*
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.*
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.SchemaAPI.*

@KtorExperimentalAPI
class LayoutFiller(private val mActivity: MainActivity) {
    private val mAdapter = PostAdapter(this)

    fun notifyDataSetChanged() {
        mAdapter.notifyDataSetChanged()
        with(mActivity) { postListing.smoothScrollToPosition(0) }
    }

    fun initViews() {
        with(mActivity) {
            showUserInfo()

            postListing.layoutManager = LinearLayoutManager(this)
            postListing.adapter = mAdapter

            swipeContainer.setOnRefreshListener {
                refreshData()
            }

            prepareNewTextPostBody()
        }
    }

    private fun MainActivity.showUserInfo() {
        val welcomeMessage = getString(R.string.welcome) + sMyself!!.username
        currentUser.text = welcomeMessage
    }

    fun prepareNewTextPostBody() {
        with(mActivity) {
            clearNewPostBody()
            textBtn.isChecked = true

            textBtn.setOnClickListener {
                prepareNewTextPostBody()
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

            sendBtn.setOnClickListener {
                val content = newContentTv.text.toString()

                if (checkContent(content)) {
                    val post = TextPost(author = sMyself!!.username, content = content)

                    sendPost(post)
                    prepareNewTextPostBody()
                }
            }

            cancelBtn.setOnClickListener {
                prepareNewTextPostBody()
            }
        }
    }

    private fun MainActivity.clearNewPostBody() {
        newContentTv.text.clear()
        newLocationGrp.visibility = GONE
        newPreviewIv.setImageURI(null)
        newPreviewIv.tag = null
        newPreviewIv.visibility = GONE
        newVideoUrlEt.text.clear()
        newVideoUrlEt.visibility = GONE
        newGalleryBtn.visibility = GONE
        newCameraBtn.visibility = GONE
        newPlayBtn.visibility = GONE
        newContainerFl.visibility = GONE
        newContainerFl.removeAllViews()
        typeGrp.visibility = VISIBLE
        newAddressEt.text.clear()
    }

    private fun checkContent(content: String): Boolean {
        val condition = content.isNotEmpty() && content.isNotBlank()

        if (!condition) {
            with(mActivity) { makeToast(this, R.string.new_post_hint) }
        }

        return condition
    }

    fun prepareNewImagePostBody() {
        with(mActivity) {
            clearNewPostBody()
            newPreviewIv.visibility = VISIBLE

            newGalleryBtn.visibility = VISIBLE
            newGalleryBtn.setOnClickListener {
                getGalleryContent()
            }

            newCameraBtn.visibility = VISIBLE
            newCameraBtn.setOnClickListener {
                getCameraContent()
            }

            sendBtn.setOnClickListener {
                val content = newContentTv.text.toString()
                val imageUrl = newPreviewIv.tag.toString()

                if (checkContent(content) && checkImageUrl(imageUrl)) {
                    val post =
                        ImagePost(
                            author = sMyself!!.username,
                            content = content,
                            imageUrl = imageUrl
                        )
                    sendPost(post)
                    prepareNewTextPostBody()
                }
            }
        }
    }

    private fun checkImageUrl(imageUrl: String): Boolean {
        val condition = imageUrl.isNotEmpty() && imageUrl.isNotBlank()

        if (!condition) {
            with(mActivity) { makeToast(this, R.string.image_uri_error_message) }
        }

        return condition
    }

    private fun MainActivity.prepareNewEventPostBody() {
        clearNewPostBody()
        newLocationGrp.visibility = VISIBLE

        sendBtn.setOnClickListener {
            val content = newContentTv.text.toString()
            val address = newAddressEt.text.toString()

            if (checkContent(content) && checkAddress(address)) {
                val post =
                    EventPost(author = sMyself!!.username, content = content, address = address)
                sendPost(post)
                prepareNewTextPostBody()
            }
        }
    }

    private fun checkAddress(address: String): Boolean {
        val condition = address.isNotEmpty() && address.isNotBlank()

        if (!condition) {
            with(mActivity) { makeToast(this, R.string.event_address_error_message) }
        }

        return condition
    }

    private fun MainActivity.prepareNewVideoPostBody() {
        clearNewPostBody()
        newVideoUrlEt.visibility = VISIBLE

        newVideoUrlEt.setOnFocusChangeListener { _, focused ->
            handleVideoUrl(focused)
        }

        sendBtn.setOnClickListener {
            val content = newContentTv.text.toString()
            val inputUrl = newVideoUrlEt.text.toString()

            if (checkContent(content) && isYoutubeUrl(inputUrl)) {
                val post =
                    VideoPost(author = sMyself!!.username, content = content, videoUrl = inputUrl)
                sendPost(post)
                prepareNewTextPostBody()
            }
        }
    }

    private fun MainActivity.handleVideoUrl(focused: Boolean) {
        if (!focused) {
            if (videoBtn.isChecked) {

                if (!updateVideoPreview()) return

                newPreviewIv.visibility = VISIBLE
                newPlayBtn.visibility = VISIBLE
                newVideoUrlEt.visibility = GONE

            } else {
                newVideoUrlEt.text.clear()
                newVideoUrlEt.visibility = GONE
            }
        }
    }

    private fun MainActivity.updateVideoPreview(): Boolean {
        val inputUrl = newVideoUrlEt.text.toString()

        return if (isYoutubeUrl(inputUrl)) {
            val url = VideoPost.parseVideoUrl(inputUrl)

            requestMedia(url, newPreviewIv)
            true

        } else {
            newVideoUrlEt.text.clear()
            false
        }
    }

    private fun isYoutubeUrl(inputUrl: String): Boolean {
        val condition = inputUrl.contains("https://www.youtube.com/watch?v=")

        if (!condition) {
            with(mActivity) { makeToast(this, R.string.video_url_error_message) }
        }

        return condition
    }

    fun initPostCardLayout(itemView: View, post: Post, positionTag: Int) {
        fun View.initLikes(post: Post, positionTag: Int) {
            likeCb.isChecked = post.liked
            updateSocialCountView(post.likes, post.liked, likesCountTv)

            likeCb.tag = positionTag
            likeCb.setOnClickListener { view: View ->
                val thisPost: Post = sRepository.getPostByPosition(view.tag as Int)

                if ((view as CheckBox).isChecked) {
                    mActivity.sendSocial(thisPost, SocialAction.LIKE, Mode.POST, view, likesCountTv)
                } else {
                    mActivity.sendSocial(thisPost, SocialAction.LIKE, Mode.DELETE, view, likesCountTv)
                }
            }
        }

        fun View.initComments(post: Post, positionTag: Int) {
            commentCb.isChecked = post.commented
            updateSocialCountView(post.comments, post.commented, commentsCountTv)

            commentCb.tag = positionTag
            commentCb.setOnClickListener { view: View ->
                val thisPost: Post = sRepository.getPostByPosition(view.tag as Int)

                if ((view as CheckBox).isChecked) {
                    //TODO: implement get comment text
                    mActivity.sendSocial(thisPost, SocialAction.COMMENT,
                        Mode.POST, view, commentsCountTv)
                } else {
                    mActivity.sendSocial(thisPost, SocialAction.COMMENT,
                        Mode.DELETE, view, commentsCountTv)
                }
            }
        }

        fun View.initShares(post: Post, positionTag: Int) {
            shareCb.isChecked = post.shared
            updateSocialCountView(post.shares, post.shared, sharesCountTv)

            shareCb.tag = positionTag
            shareCb.setOnClickListener { view: View ->
                val thisPost: Post = sRepository.getPostByPosition(view.tag as Int)

                if ((view as CheckBox).isChecked) {
                    mActivity.sendSocial(thisPost, SocialAction.SHARE, Mode.POST, view, sharesCountTv)
                } else {
                    shareCb.isChecked = post.shared // temporarily enabled
                    /* temporarily disabled
                    activity.sendSocial(thisPost, SHARE, DELETE, view, sharesCountTv)*/
                }
            }
        }

        fun View.setButtonsVisibility(post: Post) {
            if (post.isMy) {
                hideBtn.visibility = GONE
                deleteBtn.visibility = VISIBLE
                editBtn.visibility = VISIBLE
            } else {
                hideBtn.visibility = VISIBLE
                deleteBtn.visibility = GONE
                editBtn.visibility = GONE
            }
        }

        fun View.initHide(positionTag: Int) {
            hideBtn.tag = positionTag
            hideBtn.setOnClickListener { view: View ->
                val thisPost: Post = sRepository.getPostByPosition(view.tag as Int)

                mActivity.sendSocial(thisPost, SocialAction.HIDE, Mode.POST, null, null)
            }
        }

        fun View.initDeleteButton(positionTag: Int) {
            deleteBtn.tag = positionTag
            deleteBtn.setOnClickListener { view: View ->
                val thisPost: Post = sRepository.getPostByPosition(view.tag as Int)

                mActivity.requestDelete(thisPost)
            }
        }

        fun View.initEditButton(positionTag: Int) {
            editBtn.tag = positionTag
            editBtn.setOnClickListener { view: View ->
                val position = view.tag as Int
                val thisPost = sRepository.getPostByPosition(position)

                mActivity.prepareEditPostBody(thisPost, position)
            }
        }

        with(itemView) {
            initLikes(post, positionTag)
            initComments(post, positionTag)
            initShares(post, positionTag)

            setButtonsVisibility(post)
            initHide(positionTag)
            initDeleteButton(positionTag)
            initEditButton(positionTag)
        }
    }

    private fun MainActivity.prepareEditPostBody(post: Post, position: Int) {
        clearNewPostBody()
        typeGrp.visibility = GONE

        newContentTv.setText(post.content)

        when (post) {

            is TextPost -> {
                sendBtn.setOnClickListener {
                    val content = newContentTv.text.toString()

                    if (checkContent(content)) {
                        post.content = content
                        updatePostData(post, position)
                    }
                }
            }

            is ImagePost -> {
                newPreviewIv.visibility = VISIBLE
                newPlayBtn.visibility = GONE

                newPreviewIv.updateImageView(post)
                newPreviewIv.tag = post.imageUrl
                newPreviewIv.setColorFilter(Color.argb(150, 255, 255, 255))

                newGalleryBtn.visibility = VISIBLE
                newGalleryBtn.setOnClickListener {
                    getGalleryContent()
                }

                newCameraBtn.visibility = VISIBLE
                newCameraBtn.setOnClickListener {
                    getCameraContent()
                }

                sendBtn.setOnClickListener {
                    val content = newContentTv.text.toString()
                    val imageUrl = newPreviewIv.tag.toString()

                    if (checkContent(content) && checkImageUrl(imageUrl)) {
                        post.content = content
                        post.imageUrl = imageUrl
                        updatePostData(post, position)
                    }
                }
            }

            is EventPost -> {
                newLocationGrp.visibility = VISIBLE
                newAddressEt.setText(post.address)

                sendBtn.setOnClickListener {
                    val content = newContentTv.text.toString()
                    val address = newAddressEt.text.toString()

                    if (checkContent(content) && checkAddress(address)) {
                        post.content = content
                        post.address = address
                        updatePostData(post, position)
                    }
                }
            }

            is VideoPost -> {
                newPreviewIv.visibility = VISIBLE
                newPlayBtn.visibility = VISIBLE
                newVideoUrlEt.visibility = VISIBLE

                newPreviewIv.updateImageView(post)
                newVideoUrlEt.setText(post.videoUrl)

                newVideoUrlEt.setOnFocusChangeListener { _, _ ->
                    Unit
                    if (!updateVideoPreview()) return@setOnFocusChangeListener
                }

                sendBtn.setOnClickListener {
                    val content = newContentTv.text.toString()
                    val inputUrl = newVideoUrlEt.text.toString()

                    if (checkContent(content) && isYoutubeUrl(inputUrl)) {
                        post.content = content
                        post.videoUrl = inputUrl
                        updatePostData(post, position)
                    }
                }
            }

            is Repost -> {
                newContainerFl.visibility = VISIBLE

                val repostView = LayoutInflater.from(this)
                    .inflate(R.layout.repost_layout, containerFl, false)

                initPostView(repostView, sRepository.findRepostSource(post))

                repostView.findViewById<Group>(R.id.socialGrp).visibility = GONE
                repostView.adsTv.visibility = GONE

                newContainerFl.addView(repostView)

                sendBtn.setOnClickListener {
                    val content = newContentTv.text.toString()

                    if (checkContent(content)) {
                        post.content = content
                        updatePostData(post, position)
                    }
                }
            }

            is AdsPost -> {} // ignored
        }

        cancelBtn.setOnClickListener {
            prepareNewTextPostBody()
        }
    }

    private fun ImageView.updateImageView(post: Post) {
        val mediaUrl = when(post) {
            is ImagePost -> { post.imageUrl }
            is VideoPost -> { post.getImageUrl() }
            else -> { "" } // ignored
        }

        mActivity.requestMedia(mediaUrl, this)
    }

    fun initPostView(itemView: View, post: Post) {
        
        fun View.dyeBackground(colorResource: Int) {
            setBackgroundColor(ContextCompat.getColor(mActivity, colorResource))
        }

        fun View.clearFields() {
            findViewById<Group>(R.id.socialGrp).visibility = VISIBLE
            adsTv.visibility = GONE
            locationGrp.visibility = GONE
            previewIv.visibility = GONE
            playBtn.visibility = GONE
            containerFl.visibility = GONE
            containerFl.removeAllViews()
        }

        fun View.initCommonFields() {
            authorTv.text = post.author
            createdTv.text = post.age
            contentTv.text = post.content
            viewsCountTv.text = if (post.views > 0) post.views.toString() else ""
        }

        with(itemView) {
            dyeBackground(R.color.colorSecondaryBackground)
            clearFields()

            initCommonFields()

            when (post) {

                is TextPost -> {} // ignored

                is ImagePost -> {
                    previewIv.visibility = VISIBLE
                    playBtn.visibility = GONE

                    previewIv.updateImageView(post)
                }

                is EventPost -> {
                    locationGrp.visibility = VISIBLE

                    addressTv.text = post.address
                    addressTv.setOnClickListener {
                        post.open(mActivity)
                    }
                    locationIv.setOnClickListener {
                        post.open(mActivity)
                    }
                }

                is VideoPost -> {
                    previewIv.visibility = VISIBLE
                    playBtn.visibility = VISIBLE

                    previewIv.updateImageView(post)

                    playBtn.setOnClickListener {
                        post.open(mActivity)
                    }
                }

                is Repost -> {
                    containerFl.visibility = VISIBLE

                    val repostView = LayoutInflater.from(mActivity)
                        .inflate(R.layout.repost_layout, containerFl, false)

                    initPostView(repostView, sRepository.findRepostSource(post))

                    repostView.findViewById<Group>(R.id.socialGrp).visibility = GONE
                    repostView.adsTv.visibility = GONE

                    containerFl.addView(repostView)
                }

                is AdsPost -> {
                    dyeBackground(R.color.colorAdsBackground)

                    adsTv.visibility = VISIBLE
                    findViewById<Group>(R.id.socialGrp).visibility = GONE

                    contentTv.setOnClickListener {
                        post.open(mActivity)
                    }
                }
            }
        }
    }

    fun prepareNewRepostBody(post: Post, checkBox: CheckBox?, countView: TextView?) {
        with(mActivity) {
            typeGrp.visibility = GONE

            when (post) {

                is TextPost -> {
                } // ignored

                is ImagePost -> {
                    newPreviewIv.visibility = VISIBLE
                    newPlayBtn.visibility = GONE

                    newPreviewIv.updateImageView(post)
                }

                is EventPost -> {
                    newLocationGrp.visibility = VISIBLE

                    newAddressEt.setText(post.address)
                    newAddressEt.setOnClickListener {
                        post.open(this)
                    }
                    newLocationIv.setOnClickListener {
                        post.open(this)
                    }
                }

                is VideoPost -> {
                    newPreviewIv.visibility = VISIBLE
                    newPlayBtn.visibility = VISIBLE

                    newPreviewIv.updateImageView(post)

                    newPlayBtn.setOnClickListener {
                        post.open(this)
                    }
                }

                is Repost -> {
                    newContainerFl.visibility = VISIBLE

                    val repostView = LayoutInflater.from(mActivity)
                        .inflate(R.layout.repost_layout, containerFl, false)

                    initPostView(repostView, sRepository.findRepostSource(post))

                    repostView.findViewById<Group>(R.id.socialGrp).visibility = GONE
                    repostView.adsTv.visibility = GONE

                    newContainerFl.addView(repostView)
                }

                is AdsPost -> {
                } // ignored
            }

            sendBtn.setOnClickListener {
                val content = newContentTv.text.toString()

                if (checkContent(content)) {
                    post.content = content
                    sendPost(post)
                    prepareNewTextPostBody()
                }
            }

            cancelBtn.setOnClickListener {
                if (post is Repost) {
                    val sharedPost = sRepository.getPostById(post.source!!)
                    checkBox!!.isChecked = false

                    sendSocial(sharedPost, SocialAction.SHARE, Mode.DELETE, checkBox, countView)
                }

                prepareNewTextPostBody()
            }
        }
    }

    fun updateSocialCountView(count: Int, isSelected: Boolean, countView: TextView) {
        countView.text = if (count > 0) count.toString() else ""
        countView.setTextColor(
            ContextCompat.getColor(
                mActivity,
                if (isSelected) R.color.colorSelected else R.color.colorSecondaryText
            )
        )
    }
}