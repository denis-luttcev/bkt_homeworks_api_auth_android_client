package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.google.gson.Gson
import io.ktor.client.HttpClient
import io.ktor.client.features.*
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.*
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.features.BadRequestException
import io.ktor.features.UnsupportedMediaTypeException
import io.ktor.http.*
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.readUTF8Line
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

class AuthenticationException : Exception()
class AuthorizationException : Exception()
class LockedException : Exception()

data class ErrorMessage(val error: String)

@KtorExperimentalAPI
class NetworkServiceWithKtorHttpClientImpl : CoroutineScope by MainScope(), NetworkService {
    private val client = HttpClient {
        install(JsonFeature) {
            acceptContentTypes = listOf(ContentType.Application.Json)
            serializer = GsonSerializer {
                registerTypeAdapter(Post::class.java, PostDeserializer())
            }
        }
        HttpResponseValidator {
            validateResponse { response ->
                when (response.status) {
                    HttpStatusCode.Unauthorized -> throw AuthenticationException()
                    HttpStatusCode.BadRequest -> {
                        val message = Gson().fromJson(
                            response.content.readUTF8Line()!!,
                            ErrorMessage::class.java
                        )
                        throw BadRequestException(message.error)
                    }
                    HttpStatusCode.Forbidden -> throw AuthorizationException()
                    HttpStatusCode.Locked -> throw LockedException()
                    HttpStatusCode.UnsupportedMediaType ->
                        throw UnsupportedMediaTypeException(response.contentType()!!)
                }
            }
        }
    }

    override fun fetchAdapterData(dataHandler: (posts: List<Post>?, ads: List<AdsPost>?) -> Unit) {
        launch(Dispatchers.IO) {
            try {
                val postsRequest = async {
                    client.get<List<Post>>(POSTS.route) {
                        header(HttpHeaders.Authorization, "Bearer $mytoken")
                    }
                }
                val adsRequest = async {
                    client.get<List<AdsPost>>(ADS.route) {
                        header(HttpHeaders.Authorization, "Bearer $mytoken")
                    }
                }

                val posts = postsRequest.await()
                val ads = adsRequest.await()

                withContext(Dispatchers.Main) { dataHandler(posts, ads) }

            } catch (cause: AuthorizationException) {
                withContext(Dispatchers.Main) { dataHandler(null, null) }
            }
        }
    }

    override fun updatePosts(currentCounter: Int, dataHandler: (posts: List<Post>?) -> Unit) {
        launch(Dispatchers.IO) {
            try {
                val posts = client.get<List<Post>>(POSTS.routeWith(currentCounter)) {
                    header(HttpHeaders.Authorization, "Bearer $mytoken")
                }

                withContext(Dispatchers.Main) { dataHandler(posts) }

            } catch (cause: AuthorizationException) {
                withContext(Dispatchers.Main) { dataHandler(null) }
            }
        }
    }

    override fun updateAds(currentCounter: Int, dataHandler: (ads: List<AdsPost>?) -> Unit) {
        launch(Dispatchers.IO) {
            try {
                val ads = client.get<List<AdsPost>>(ADS.routeWith(currentCounter)) {
                    header(HttpHeaders.Authorization, "Bearer $mytoken")
                }

                withContext(Dispatchers.Main) { dataHandler(ads) }

            } catch (cause: AuthorizationException) {
                withContext(Dispatchers.Main) { dataHandler(null) }
            }
        }
    }

    override fun savePost(post: Post, completionListener: (successfully: Boolean) -> Unit) {
        launch(Dispatchers.IO) {
            try {
                val permanentID = client.post<UUID>(POSTS.route) {
                    header(HttpHeaders.Authorization, "Bearer $mytoken")
                    contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                    body = Gson().toJsonTree(Post.fromModel(post))
                }

                post.id = permanentID

                withContext(Dispatchers.Main) { completionListener(true) }

            } catch (cause: AuthorizationException) {
                withContext(Dispatchers.Main) { completionListener(false) }
            }
        }
    }

    override fun updatePost(post: Post, completionListener: (successfully: Boolean) -> Unit) {
        launch(Dispatchers.IO) {
            try {
                client.post<String>(POSTS.routeWith(post.id)) {
                    header(HttpHeaders.Authorization, "Bearer $mytoken")
                    contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                    body = Gson().toJsonTree(Post.fromModel(post))
                }

                withContext(Dispatchers.Main) { completionListener(true) }

            } catch (cause: AuthorizationException) {
                withContext(Dispatchers.Main) { completionListener(false) }
            }
        }
    }

    override fun deletePost(postID: UUID, completionListener: (successfully: Boolean) -> Unit) {
        launch(Dispatchers.IO) {
            try {
                client.delete<String>(POSTS.routeWith(postID)) {
                    header(HttpHeaders.Authorization, "Bearer $mytoken")
                }

                withContext(Dispatchers.Main) { completionListener(true) }

            } catch (cause: AuthorizationException) {
                withContext(Dispatchers.Main) { completionListener(false) }
            }
        }
    }

    override fun saveMedia(
        mediaUri: Uri,
        context: Context,
        dataHandler: (permanentUrl: String?, cause: Throwable?) -> Unit
    ) {

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
        } catch (cause : Exception) {
            dataHandler(null, cause)
            return
        }

        val contentType = when(filename!!.split(".")[1].toLowerCase(Locale.getDefault())) {
            "jpeg" -> ContentType.Image.JPEG
            "jpg" -> ContentType.Image.JPEG
            "png" -> ContentType.Image.PNG
            else -> ContentType.Image.Any
        }

        launch(Dispatchers.IO) {
            try {
                val media = client.post<Media>(MEDIA.route) {
                    header(HttpHeaders.Authorization, "Bearer $mytoken")
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
                                            .withParameter(
                                                ContentDisposition.Parameters.Name,
                                                "file"
                                            )
                                            .withParameter(
                                                ContentDisposition.Parameters.FileName,
                                                filename
                                            )
                                            .toString()
                                    )
                                }
                            )
                        }
                    )
                }

                withContext(Dispatchers.Main) { dataHandler(media.imageUrl, null) }

            } catch (cause: AuthorizationException) {
                withContext(Dispatchers.Main) { dataHandler(null, cause) }
            } catch (cause: UnsupportedMediaTypeException) {
                withContext(Dispatchers.Main) { dataHandler(null, cause) }
            }
        }
    }

    override fun updateSocial(
        postID: UUID,
        action: SocialAction,
        mode: Mode,
        completionListener: (cause: Throwable?) -> Unit
    ) {

        launch(Dispatchers.IO) {
            val url = POSTS.routeWith(postID, action)

            try {
                when (mode) {
                    Mode.POST -> client.post<String>(url) {
                        header(HttpHeaders.Authorization, "Bearer $mytoken")
                    }
                    Mode.DELETE -> client.delete<String>(url) {
                        header(HttpHeaders.Authorization, "Bearer $mytoken")
                    }
                }

                withContext(Dispatchers.Main) { completionListener(null) }

            } catch (cause: AuthorizationException) {
                withContext(Dispatchers.Main) { completionListener(cause) }
            } catch (cause: LockedException) {
                withContext(Dispatchers.Main) { completionListener(cause) }
            }
        }
    }

    override fun loadMedia(mediaUrl: String, dataHandler: (image: Bitmap?) -> Unit) {
        launch(Dispatchers.IO) {
            try {
                val connection = URL(mediaUrl).openConnection()
                connection.setRequestProperty(HttpHeaders.Authorization, "Bearer $mytoken")

                val image = BitmapFactory.decodeStream(connection.getInputStream())

                withContext(Dispatchers.Main) { dataHandler(image) }

            } catch (cause: AuthorizationException) {
                withContext(Dispatchers.Main) { dataHandler(null) }
            }
        }
    }

    override fun registrate(
        username: String,
        login: String,
        password: String,
        dataHandler: (token: String?, message: String?) -> Unit
    ) {
        launch(Dispatchers.IO) {
            val request = User.RegistrationRequestDto(username, login, password)

            try {
                val response = client.post<User.AuthenticationResponseDto> {
                    url(REGISTRATION.route)
                    contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                    body = Gson().toJsonTree(request)
                }

                withContext(Dispatchers.Main) { dataHandler(response.token, null) }
            } catch (cause: BadRequestException) {
                withContext(Dispatchers.Main) { dataHandler(null, cause.message) }
            }
        }
    }

    override fun authenticate(login: String, password: String, dataHandler: (token: String?) -> Unit) {
        launch(Dispatchers.IO) {
            val request = User.AuthenticationRequestDto(login, password)

            try {
                val response = client.post<User.AuthenticationResponseDto> {
                    url(AUTHENTICATION.route)
                    contentType(ContentType.Application.Json.withCharset(Charsets.UTF_8))
                    body = Gson().toJsonTree(request)
                }

                withContext(Dispatchers.Main) { dataHandler(response.token) }
            } catch (cause: AuthenticationException) {
                withContext(Dispatchers.Main) { dataHandler(null) }
            }
        }
    }

    override fun getMe(dataHandler: (user: User?) -> Unit) {
        launch(Dispatchers.IO) {

            try {
                val me = client.get<User>(ME.route) {
                    header(HttpHeaders.Authorization, "Bearer $mytoken")
                }

                withContext(Dispatchers.Main) { dataHandler(me) }
            } catch (cause: AuthorizationException) {
                withContext(Dispatchers.Main) { dataHandler(null) }
            }
        }
    }

    override fun cancellation() {
        this.coroutineContext.cancelChildren()
    }
}