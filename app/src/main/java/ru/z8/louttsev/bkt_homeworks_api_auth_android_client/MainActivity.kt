package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import io.ktor.util.KtorExperimentalAPI

private const val GALLERY_REQUEST = 100
private const val CAMERA_REQUEST = 101

@KtorExperimentalAPI
class MainActivity : AppCompatActivity() {
    private val filler = LayoutFiller(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        filler.initViews()
    }

    override fun onStop() {
        super.onStop()
        networkService.cancellation()
    }

    fun startLoginActivity() {
        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
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

    fun getImageContent() {
        startActivityForResult(Intent().apply {
            action = Intent.ACTION_GET_CONTENT
            type = "image/*"
        }, GALLERY_REQUEST)
    }

    fun getCameraContent() {
        //TODO: implement make new image by camera
    }
}