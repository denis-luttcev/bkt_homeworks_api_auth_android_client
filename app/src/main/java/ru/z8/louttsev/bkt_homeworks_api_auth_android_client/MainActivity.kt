package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
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

const val postsUrl = "https://api-auth-server-luttcev.herokuapp.com/api/v1/posts"
const val adsUrl = "https://api-auth-server-luttcev.herokuapp.com/api/v1/ads"

@KtorExperimentalAPI
class MainActivity : AppCompatActivity(), CoroutineScope by MainScope() {
    private lateinit var postAdapter : PostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fetchData()

        prepareNewPostBody()

        swipeContainer.setOnRefreshListener {
            postAdapter.updateData()
            swipeContainer.isRefreshing = false
        }
    }

    private fun prepareNewPostBody() {
        with(newPostLayout) {
            newLocationGrp.visibility = View.GONE
            newPreviewIv.visibility = View.GONE
            newPlayBtn.visibility = View.GONE
            newContainerFl.visibility = View.GONE
            newContainerFl.removeAllViews()
            sendBtn.setOnClickListener {
                val newPost = TextPost(author = "Netology", content = newContentTv.text.toString())
                postAdapter.savePost(newPost)
                newContentTv.text.clear()
            }
        }
    }

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

    fun fillNewPostBody(post: Post) {
        with(newPostLayout) {
            this.setBackground(getDrawable(R.drawable.rounded_block))
            when (post) {
                is EventPost -> {
                    newLocationGrp.visibility = View.VISIBLE
                    newAddressTv.text = post.address
                    newAddressTv.setOnClickListener {
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
                post.content = newContentTv.text.toString()
                postAdapter.savePost(post)
                newContentTv.text.clear()
                prepareNewPostBody()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
