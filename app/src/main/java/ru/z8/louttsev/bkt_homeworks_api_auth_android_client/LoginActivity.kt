package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_login.*

@KtorExperimentalAPI
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val mainIntent = Intent(this, MainActivity::class.java)

        if (mytoken != null) {
            startActivity(mainIntent)
            finish()
        }

        loginBtn.setOnClickListener {
            networkService.authenticate(
                loginEdt.text.toString(),
                passwordEdt.text.toString()

            ) { token ->
                mytoken = token

                networkService.getMe { user ->
                    myself = user

                    startActivity(mainIntent)
                    finish()
                }
            }
        }

        //TODO: implement registration
    }
}
