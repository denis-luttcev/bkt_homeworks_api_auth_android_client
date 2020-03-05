package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.http.*
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.User
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.AdsPost
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.Media
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.Post
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.PostDeserializer
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.mytoken
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.SchemaAPI.*
import java.net.URL
import java.util.*

@KtorExperimentalAPI
class NetworkServiceWithKtorHttpClientImpl : CoroutineScope by MainScope(), NetworkService {
    private val client = HttpClient {
        install(JsonFeature) {
            acceptContentTypes = listOf(ContentType.Application.Json)
            serializer = GsonSerializer {
                registerTypeAdapter(Post::class.java, PostDeserializer())
            }
        }
    }

    override fun fetchAdapterData(dataHandler: (posts: List<Post>, ads: List<AdsPost>) -> Unit) {
        launch(Dispatchers.IO) {
            val postsRequest = async {
                client.get<List<Post>>(POSTS.route) {
                    header(HttpHeaders.Authorization, "Bearer ${mytoken}")
                }
            }
            val adsRequest = async {
                client.get<List<AdsPost>>(ADS.route) {
                    header(HttpHeaders.Authorization, "Bearer ${mytoken}")
                }
            }

            val posts = postsRequest.await()
            val ads = adsRequest.await()

            withContext(Dispatchers.Main) { dataHandler(posts, ads) }
        }
    }

    override fun updatePosts(currentCounter: Int, dataHandler: (posts: List<Post>) -> Unit) {
        launch(Dispatchers.IO) {
            val posts = client.get<List<Post>>(POSTS.routeWith(currentCounter)) {
                header(HttpHeaders.Authorization, "Bearer ${mytoken}")
            }

            withContext(Dispatchers.Main) { dataHandler(posts) }
        }
    }

    override fun updateAds(currentCounter: Int, dataHandler: (ads: List<AdsPost>) -> Unit) {
        launch(Dispatchers.IO) {
            val ads = client.get<List<AdsPost>>(ADS.routeWith(currentCounter)) {
                header(HttpHeaders.Authorization, "Bearer ${mytoken}")
            }

            withContext(Dispatchers.Main) { dataHandler(ads) }
        }
    }

    override fun savePost(post: Post, completionListener: () -> Unit) {
        launch(Dispatchers.IO) {
            val permanentID = client.post<UUID>(POSTS.route) {
                header(HttpHeaders.Authorization, "Bearer ${mytoken}")
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                body = Gson().toJsonTree(Post.fromModel(post))
            }

            post.id = permanentID

            withContext(Dispatchers.Main) { completionListener() }
        }
    }

    override fun saveMedia(mediaUri: Uri, context: Context, dataHandler: (permanentUrl: String) -> Unit) {
        val filename = try {
            context.contentResolver.query(
                mediaUri,
                null,
                null,
                null,
                null
            )?.let { cursor ->
                cursor.run {
                    if (moveToFirst()) getString(getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    else null
                }.also { cursor.close() }
            }
        } catch (e : Exception) { null }

        val contentType = when(filename!!.split(".")[1].toLowerCase(Locale.getDefault())) {
            "jpeg" -> ContentType.Image.JPEG
            "jpg" -> ContentType.Image.JPEG
            "png" -> ContentType.Image.PNG
            else -> ContentType.Image.Any
        }

        launch(Dispatchers.IO) {
            val media = client.post<Media>(MEDIA.route) {
                header(HttpHeaders.Authorization, "Bearer ${mytoken}")
                body = MultiPartFormDataContent(
                    formData {
                        append(
                            key = "file",
                            value = context.contentResolver.openInputStream(mediaUri)!!.readBytes(),
                            headers = Headers.build {
                                append(HttpHeaders.ContentType, contentType)
                                append(
                                    HttpHeaders.ContentDisposition,
                                    ContentDisposition.File
                                        .withParameter(ContentDisposition.Parameters.Name, "file")
                                        .withParameter(ContentDisposition.Parameters.FileName, filename)
                                        .toString()
                                )
                            }
                        )
                    }
                )
            }

            withContext(Dispatchers.Main) { dataHandler(media.imageUrl) }
        }
    }

    override fun updateSocial(postID: UUID, action: SocialAction, mode: Mode) {
        launch(Dispatchers.IO) {
            val url = POSTS.routeWith(postID, action)

            when (mode) {
                Mode.POST -> client.post<String>(url) {
                    header(HttpHeaders.Authorization, "Bearer ${mytoken}")
                }
                Mode.DELETE -> client.delete<String>(url) {
                    header(HttpHeaders.Authorization, "Bearer ${mytoken}")
                }
            }
        }
    }

    override fun loadMedia(mediaUrl: String, dataHandler: (image: Bitmap) -> Unit) {
        launch(Dispatchers.IO) {
            val connection = URL(mediaUrl).openConnection()
            connection.setRequestProperty(HttpHeaders.Authorization, "Bearer ${mytoken}")

            val image = BitmapFactory.decodeStream(connection.getInputStream())

            withContext(Dispatchers.Main) { dataHandler(image) }
        }
    }

    override fun authenticate(login: String, password: String, dataHandler: (token: String) -> Unit) {
        launch(Dispatchers.IO) {
            val request = User.AuthenticationRequestDto(login, password)

            val response = client.post<User.AuthenticationResponseDto> {
                url(AUTHENTICATION.route)
                contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                body = Gson().toJsonTree(request)
            }

            withContext(Dispatchers.Main) { dataHandler(response.token) }
        }
    }

    override fun getMe(dataHandler: (user: User) -> Unit) {
        launch(Dispatchers.IO) {
            val me = client.get<User>(ME.route) {
                header(HttpHeaders.Authorization, "Bearer ${mytoken}")
            }

            withContext(Dispatchers.Main) { dataHandler(me) }
        }
    }

    override fun cancellation() {
        cancel()
        //client.close()
    }
}