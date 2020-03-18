package ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.User
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.AdsPost
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.Post
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.SchemaAPI.*
import java.util.*

interface NetworkService {
    fun fetchData(dataHandler: (posts: List<Post>?, ads: List<AdsPost>?) -> Unit)
    fun appendPosts(currentCounter: Int, dataHandler: (posts: List<Post>?) -> Unit)
    fun appendAds(currentCounter: Int, dataHandler: (ads: List<AdsPost>?) -> Unit)
    fun savePost(post: Post, completionListener: (successfully: Boolean) -> Unit)
    fun updatePost(post: Post, completionListener: (successfully: Boolean) -> Unit)
    fun deletePost(postID: UUID, completionListener: (successfully: Boolean) -> Unit)
    fun saveMedia(
        mediaUri: Uri,
        context: Context,
        dataHandler: (permanentUrl: String?, cause: Throwable?) -> Unit)
    fun updateSocial(
        postID: UUID,
        action: SocialAction,
        mode: Mode,
        completionListener: (cause: Throwable?) -> Unit)
    fun loadMedia(mediaUrl: String, dataHandler: (image: Bitmap?) -> Unit)
    fun registrate(
        username: String,
        login: String,
        password: String,
        dataHandler: (token: String?, message: String?) -> Unit)
    fun authenticate(login: String, password: String, dataHandler: (token: String?) -> Unit)
    fun getMe(dataHandler: (user: User?) -> Unit)
    fun cancellation()
}