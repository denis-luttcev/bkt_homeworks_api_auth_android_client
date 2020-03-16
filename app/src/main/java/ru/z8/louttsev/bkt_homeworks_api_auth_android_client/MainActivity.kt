package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.recyclerview.widget.LinearLayoutManager
import io.ktor.features.UnsupportedMediaTypeException
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.new_post_layout.*
import kotlinx.android.synthetic.main.new_post_layout.view.*
import kotlinx.android.synthetic.main.post_card_layout.view.*
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.*
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.parseVideoUrl
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.AuthorizationException
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.LockedException
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.SchemaAPI.*

private const val GALLERY_REQUEST = 100

@KtorExperimentalAPI
class MainActivity : AppCompatActivity() {
    private val postAdapter = PostAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        postListing.layoutManager = LinearLayoutManager(this)
        postListing.adapter = postAdapter

        swipeContainer.isRefreshing = true

        networkService.fetchAdapterData { posts: List<Post>?, ads: List<AdsPost>? ->
            if (posts != null && ads != null) {
                repository.addPosts(posts)
                repository.addAds(ads)

                postAdapter.notifyDataSetChanged()

                swipeContainer.isRefreshing = false

            } else {
                handleAuthorizationException()
            }
        }

        swipeContainer.setOnRefreshListener {
            networkService.updateAds(repository.getAdsCount()) {
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

    override fun onDestroy() {
        super.onDestroy()

        networkService.cancellation()
    }

    fun handleAuthorizationException() {
        Toast.makeText(
            this,
            getString(R.string.authorization_error_message),
            Toast.LENGTH_SHORT
        ).show()

        swipeContainer.isRefreshing = false

        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
        finish()
    }

    private fun updatePostsInAdapter(successfully: Boolean) {
        if (!successfully) {
            handleAuthorizationException()
            return
        }

        swipeContainer.isRefreshing = true
        networkService.updatePosts(repository.getPostsCount()) {
            if (it != null) {
                repository.addPosts(it)
                postAdapter.notifyDataSetChanged()

                postListing.smoothScrollToPosition(0)

                swipeContainer.isRefreshing = false

            } else {
                handleAuthorizationException()
            }
        }
    }

    private fun prepareNewTextPostBody() {
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

    private fun clearNewPostBody() {
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

    private fun prepareNewImagePostBody() {
        clearNewPostBody()

        newPreviewIv.visibility = View.VISIBLE

        newGalleryBtn.visibility = View.VISIBLE
        newGalleryBtn.setOnClickListener {
            startActivityForResult(Intent().apply {
                action = Intent.ACTION_GET_CONTENT
                type = "image/*"
            }, GALLERY_REQUEST)
        }

        newCameraBtn.visibility = View.VISIBLE
        newCameraBtn.setOnClickListener {
            //TODO: implement make new image by camera
            //TODO: implement update image preview
        }

        sendBtn.setOnClickListener {
            val content = newContentTv.text.toString()
            if (content.isEmpty() || content.isBlank()) {
                Toast.makeText(this,
                    R.string.new_post_hint,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val imageUrl = newPreviewIv.tag.toString()
            if (imageUrl.isEmpty() || imageUrl.isBlank()) {
                Toast.makeText(this,
                    R.string.image_uri_error_message,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val post = ImagePost(author = myself!!.username, content = content, imageUrl = imageUrl)

            networkService.savePost(post, ::updatePostsInAdapter)

            prepareNewTextPostBody()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            GALLERY_REQUEST -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val imageUri = data.data!!

                    newPreviewIv.setImageURI(imageUri)

                    newGalleryBtn.visibility = View.GONE
                    newCameraBtn.visibility = View.GONE

                    networkService.saveMedia(imageUri, this) { url: String?, cause: Throwable? ->
                        if (url != null) {
                            newPreviewIv.tag = url
                        } else {
                            when(cause) {
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
            //TODO: implement camera image handle
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun prepareNewEventPostBody() {
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
                Toast.makeText(this,
                    R.string.event_address_error_message,
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val post = EventPost(author = myself!!.username, content = content, address = address)

            networkService.savePost(post, ::updatePostsInAdapter)

            prepareNewTextPostBody()
        }
    }

    private fun prepareNewVideoPostBody() {
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

            val post = VideoPost(author = myself!!.username, content = content, videoUrl = inputUrl)

            networkService.savePost(post, ::updatePostsInAdapter)

            prepareNewTextPostBody()
        }
    }

    private fun handleVideoUrl(view: View, focused: Boolean) {
        if (!focused) {
            if (videoBtn.isChecked) {

                val inputUrl = newVideoUrlEt.text.toString()
                if (isNotYouTubeUrl(inputUrl)) {
                    Toast.makeText(
                        this,
                        R.string.video_url_error_message,
                        Toast.LENGTH_SHORT
                    ).show()
                    newVideoUrlEt.text.clear()
                    return
                }

                networkService.loadMedia(parseVideoUrl(inputUrl)) {
                    if (it == null) handleAuthorizationException()

                    newPreviewIv.setImageBitmap(it)
                }

                newPreviewIv.visibility = View.VISIBLE
                newPlayBtn.visibility = View.VISIBLE
                newVideoUrlEt.visibility = View.GONE

            } else {
                newVideoUrlEt.text.clear()
                newVideoUrlEt.visibility = View.GONE
            }
        }
    }

    private fun isNotYouTubeUrl(inputUrl: String)
            = !inputUrl.contains("https://www.youtube.com/watch?v=")

    fun prepareNewRepostBody(post: Post, view: CheckBox?, countView: TextView?) {
        with(newPostLayout) {

            typeGrp.visibility = View.GONE

            when (post) {
                is EventPost -> {
                    newLocationGrp.visibility = View.VISIBLE
                    newAddressEt.setText(post.address)
                    newAddressEt.setOnClickListener {
                        post.open(context)
                    }
                    newLocationIv.setOnClickListener {
                        post.open(context)
                    }
                }

                is VideoPost -> {
                    newPreviewIv.visibility = View.VISIBLE

                    newPlayBtn.visibility = View.VISIBLE
                    newPlayBtn.setOnClickListener {
                        post.open(context)
                    }

                    postAdapter.getLayoutFiller().updateImageView(post, newPreviewIv)
                }

                is Repost -> {
                    newContainerFl.visibility = View.VISIBLE

                    val repostView = LayoutInflater.from(context)
                        .inflate(R.layout.repost_layout, containerFl, false)
                    postAdapter.getLayoutFiller()
                        .initPostView(
                            repostView as ConstraintLayout,
                            repository.findRepostSource(post)
                        )

                    repostView.findViewById<Group>(R.id.socialGrp).visibility = View.GONE
                    repostView.adsTv.visibility = View.GONE

                    newContainerFl.addView(repostView)
                }

                is ImagePost -> {
                    newPreviewIv.visibility = View.VISIBLE
                    newPlayBtn.visibility = View.GONE

                    postAdapter.getLayoutFiller().updateImageView(post, previewIv)
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
                                    this@MainActivity,
                                    R.string.locked_error_message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            null -> {
                                sharedPost.removeShare()
                                view!!.isChecked = false

                                postAdapter.getLayoutFiller()
                                    .updateSocialCountView(sharedPost.shares, false, countView!!)
                            }
                        }
                    }
                }

                prepareNewTextPostBody()
            }
        }
    }

    fun prepareEditPostBody(post: Post, position: Int) {
        with(newPostLayout) {
            clearNewPostBody()
            typeGrp.visibility = View.GONE

            newContentTv.setText(post.content)

            when (post) {
                is TextPost -> {
                    sendBtn.setOnClickListener {
                        val content = newContentTv.text.toString()
                        if (content.isEmpty() || content.isBlank()) {
                            Toast.makeText(
                                this@MainActivity,
                                R.string.new_post_hint,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        post.content = content
                        update(post, position)
                    }
                }

                is EventPost -> {
                    newLocationGrp.visibility = View.VISIBLE

                    newAddressEt.setText(post.address)

                    sendBtn.setOnClickListener {
                        val content = newContentTv.text.toString()
                        if (content.isEmpty() || content.isBlank()) {
                            Toast.makeText(
                                this@MainActivity,
                                R.string.new_post_hint,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        val address = newAddressEt.text.toString()
                        if (address.isEmpty() || address.isBlank()) {
                            Toast.makeText(this@MainActivity,
                                R.string.event_address_error_message,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        post.content = content
                        post.address = address
                        update(post, position)
                    }
                }

                is VideoPost -> {
                    newPreviewIv.visibility = View.VISIBLE
                    newPlayBtn.visibility = View.VISIBLE

                    postAdapter.getLayoutFiller().updateImageView(post, newPreviewIv)

                    //TODO: refill videoUrl EditText

                    newVideoUrlEt.setOnFocusChangeListener(::handleVideoUrl)

                    sendBtn.setOnClickListener {
                        val content = newContentTv.text.toString()
                        if (content.isEmpty() || content.isBlank()) {
                            Toast.makeText(
                                this@MainActivity,
                                R.string.new_post_hint,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        val inputUrl = newVideoUrlEt.text.toString()
                        if (isNotYouTubeUrl(inputUrl)) {
                            Toast.makeText(
                                this@MainActivity,
                                R.string.new_post_hint,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        post.content = content
                        post.videoUrl = inputUrl
                        update(post, position)
                    }
                }

                is Repost -> {
                    newContainerFl.visibility = View.VISIBLE

                    val repostView = LayoutInflater.from(context)
                        .inflate(R.layout.repost_layout, containerFl, false)
                    postAdapter.getLayoutFiller()
                        .initPostView(
                            repostView as ConstraintLayout,
                            repository.findRepostSource(post)
                        )

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
                        update(post, position)
                    }
                }

                is ImagePost -> {
                    newPreviewIv.visibility = View.VISIBLE
                    newPlayBtn.visibility = View.GONE

                    postAdapter.getLayoutFiller().updateImageView(post, newPreviewIv)

                    //TODO: show sources buttons

                    sendBtn.setOnClickListener {
                        val content = newContentTv.text.toString()
                        if (content.isEmpty() || content.isBlank()) {
                            Toast.makeText(this@MainActivity,
                                R.string.new_post_hint,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        val imageUrl = newPreviewIv.tag.toString()
                        if (imageUrl.isEmpty() || imageUrl.isBlank()) {
                            Toast.makeText(this@MainActivity,
                                R.string.image_uri_error_message,
                                Toast.LENGTH_SHORT
                            ).show()
                            return@setOnClickListener
                        }

                        post.content = content
                        post.imageUrl = imageUrl
                        update(post, position)
                    }
                }

                else -> {} // ignored
            }

            cancelBtn.setOnClickListener {
                prepareNewTextPostBody()
            }
        }
    }

    private fun update(post: Post, position: Int) {
        //TODO: implement update request
        //networkService.savePost(post, ::updatePostsInAdapter)

        postAdapter.notifyDataSetChanged()
        postListing.smoothScrollToPosition(position)
        prepareNewTextPostBody()
    }
}