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
import io.ktor.client.HttpClient
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.new_post_layout.*
import kotlinx.android.synthetic.main.new_post_layout.view.*
import kotlinx.android.synthetic.main.post_card_layout.view.*
import kotlinx.coroutines.*
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.*
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.parseVideoUrl

const val postsUrl = "https://api-auth-server-luttcev.herokuapp.com/api/v1/posts"
const val adsUrl = "https://api-auth-server-luttcev.herokuapp.com/api/v1/ads"
const val GALLERY_REQUEST = 100

@KtorExperimentalAPI
class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {

    private lateinit var postAdapter : PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fetchData()

        prepareNewTextPostBody()

        swipeContainer.setOnRefreshListener {
            postAdapter.updateData()
            swipeContainer.isRefreshing = false
        }
    }

    private fun prepareNewTextPostBody() {
        with(newPostLayout) {
            clearNewPostBody()
            textBtn.isChecked = true
            sendBtn.setOnClickListener {
                val content = newContentTv.text.toString()
                if (content.isNotEmpty() && content.isNotBlank()) {
                    val newPost = TextPost(author = "Netology", content = content)
                    postAdapter.savePost(newPost)
                    clearNewPostBody()
                } else
                    Toast.makeText(this@MainActivity,
                            R.string.new_post_hint,
                            Toast.LENGTH_SHORT
                        ).show()
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
            val imageUrl = newPreviewIv.tag.toString()
            if (content.isNotEmpty() && content.isNotBlank() && imageUrl.isNotEmpty()) {
                val newPost = ImagePost(
                    author = "Netology",
                    content = content,
                    imageUrl = imageUrl
                )
                postAdapter.savePost(newPost)
                prepareNewTextPostBody()
            } else
                Toast.makeText(this@MainActivity,
                    R.string.new_post_hint,
                    Toast.LENGTH_SHORT
                ).show()
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
                    postAdapter.saveMedia(imageUri, newPreviewIv)
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
            val address = newAddressEt.text.toString()
            if (content.isNotEmpty()
                && content.isNotBlank()
                && address.isNotEmpty()
                && address.isNotBlank()
            ) {
                val newPost = EventPost(author = "Netology", content = content, address = address)
                postAdapter.savePost(newPost)
                prepareNewTextPostBody()
            } else
                Toast.makeText(this@MainActivity,
                    R.string.new_post_hint,
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    private fun prepareNewVideoPostBody() {
        clearNewPostBody()
        newVideoUrlEt.visibility = View.VISIBLE
        newVideoUrlEt.setOnFocusChangeListener { _, focused ->
            if (!focused) {
                if (videoBtn.isChecked) {
                    val inputUrl = newVideoUrlEt.text.toString()
                    if (isYouTubeUrl(inputUrl)) {
                        postAdapter.asyncUpdatePreview(parseVideoUrl(inputUrl), newPreviewIv)
                        newPreviewIv.visibility = View.VISIBLE
                        newPlayBtn.visibility = View.VISIBLE
                        newVideoUrlEt.visibility = View.GONE
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            R.string.video_url_error_message,
                            Toast.LENGTH_SHORT
                        ).show()
                        newVideoUrlEt.text.clear()
                    }
                } else {
                    newVideoUrlEt.text.clear()
                    newVideoUrlEt.visibility = View.GONE
                }
            }
        }
        sendBtn.setOnClickListener {
            val content = newContentTv.text.toString()
            val inputUrl = newVideoUrlEt.text.toString()
            if (content.isNotEmpty() && content.isNotBlank() && isYouTubeUrl(inputUrl)) {
                val newPost = VideoPost(author = "Netology", content = content, videoUrl = inputUrl)
                postAdapter.savePost(newPost)
                prepareNewTextPostBody()
            } else
                Toast.makeText(this@MainActivity,
                    R.string.new_post_hint,
                    Toast.LENGTH_SHORT
                ).show()
        }
    }

    private fun isYouTubeUrl(inputUrl: String)
            = inputUrl.contains("https://www.youtube.com/watch?v=")

    private fun fetchData() = launch {
        indeterminateBar.visibility = View.VISIBLE

        lateinit var postList : MutableList<Post>
        lateinit var adsList : MutableList<AdsPost>

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
            val postsRequest = async { client.get<List<Post>>(postsUrl) }
            val adsRequest = async { client.get<List<AdsPost>>(adsUrl) }

            postList = postsRequest.await().toMutableList()
            adsList = adsRequest.await().toMutableList()

            client.close()
        }

        postAdapter = PostAdapter(postList, adsList, postListing)

        with(postListing) {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = postAdapter
        }

        indeterminateBar.visibility = View.GONE
    }

    fun fillNewPostBody(post: Post, view: CheckBox?, countView: TextView?) {
        with(newPostLayout) {
            this.setBackground(getDrawable(R.drawable.rounded_block))
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
                    postAdapter.asyncUpdatePreview(post, newPreviewIv)
                    newPlayBtn.setOnClickListener {
                        post.open(context)
                    }
                }
                is Repost -> {
                    newContainerFl.visibility = View.VISIBLE
                    val repostView = LayoutInflater.from(context)
                        .inflate(R.layout.repost_layout, containerFl, false)
                    postAdapter.initPostView(repostView as ConstraintLayout, postAdapter.findSource(post))
                    repostView.findViewById<Group>(R.id.socialGrp).visibility = View.GONE
                    repostView.adsTv.visibility = View.GONE
                    newContainerFl.addView(repostView)
                }
                is ImagePost -> {
                    newPreviewIv.visibility = View.VISIBLE
                    newPlayBtn.visibility = View.GONE
                    postAdapter.asyncUpdatePreview(post, previewIv)
                }
                else -> {} // ignored
            }
            sendBtn.setOnClickListener {
                val content = newContentTv.text.toString()
                if (content.isNotEmpty() && content.isNotBlank()) {
                    post.content = content
                } else {
                    post.content = getString(R.string.repost_text_default)
                }
                postAdapter.savePost(post)
                prepareNewTextPostBody()
            }
            cancelBtn.setOnClickListener {
                if (post is Repost) {
                    val sharedPost = postAdapter.getPostById(post.source!!)
                    sharedPost!!.removeShare()
                    postAdapter.asyncUpdateSocial(post.source!!, "share", Mode.DELETE)
                    view!!.isChecked = false
                    postAdapter.updateSocialCountView(sharedPost.shares, false, countView!!)
                }
                prepareNewTextPostBody()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
