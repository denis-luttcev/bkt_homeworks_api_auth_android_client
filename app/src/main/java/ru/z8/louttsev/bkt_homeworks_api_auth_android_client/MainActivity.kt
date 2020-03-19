package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast

import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_main.*
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.AdsPost
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.datamodel.Post

private const val GALLERY_REQUEST = 100
private const val CAMERA_REQUEST = 101

@KtorExperimentalAPI
class MainActivity : AppCompatActivity() {
    private val filler = LayoutFiller(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loadInitData()
        filler.initViews()
    }

    override fun onStart() {
        super.onStart()

        refreshData()
    }

    override fun onStop() {
        super.onStop()

        cancelRequests()
    }

    private fun cancelRequests() {
        networkService.cancellation()
    }

    private fun loadInitData() {
        swipeContainer.isRefreshing = true

        networkService.fetchData { posts: List<Post>?, ads: List<AdsPost>? ->
            if (posts != null && ads != null) {
                repository.addPosts(posts)
                repository.addAds(ads)

                filler.notifyDataSetChanged()

                swipeContainer.isRefreshing = false

            } else {
                handleAuthorizationException()
            }
        }
    }

    private fun refreshData() {

    }

    private fun handleAuthorizationException() {
        makeToast(this, R.string.authorization_error_message)
        swipeContainer.isRefreshing = false
        startLoginActivity()
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            GALLERY_REQUEST -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val imageUri = data.data!!
                    filler.handleGalleryImage(imageUri)
                }
            }
            CAMERA_REQUEST -> {
                //TODO: implement get camera image
                filler.handleCameraImage()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun startLoginActivity() {
        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
    }

    fun getGalleryContent() {
        startActivityForResult(Intent().apply {
            action = Intent.ACTION_GET_CONTENT
            type = "image/*"
        }, GALLERY_REQUEST)
    }

    fun getCameraContent() {
        //TODO: implement make new image by camera
    }
}