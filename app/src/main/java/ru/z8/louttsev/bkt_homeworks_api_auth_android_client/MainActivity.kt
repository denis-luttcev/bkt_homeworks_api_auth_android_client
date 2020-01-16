package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import io.ktor.client.HttpClient
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.jetbrains.anko.sdk27.coroutines.onClick
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

        sendBtn.onClick {
            val newPost = TextPost(author = "Netology", content = newPostTv.text.toString())
            postAdapter.savePost(newPost)
            postAdapter.updateData()
            newPostTv.text.clear()
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

        postAdapter = PostAdapter(postList)//.diluteWithAds(adsList)

        with(postListing) {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = postAdapter
        }

        indeterminateBar.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }
}
