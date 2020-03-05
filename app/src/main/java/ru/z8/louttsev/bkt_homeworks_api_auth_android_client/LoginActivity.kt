package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_login.*
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.AuthenticationException

@KtorExperimentalAPI
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (mytoken != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        loginBtn.setOnClickListener(::login)

        //TODO: implement registration
    }

    private fun login(view: View) {
        networkService.authenticate(
            loginEdt.text.toString(),
            passwordEdt.text.toString(),
            ::checkAuthentication
        )
    }

    private fun checkAuthentication(token: String?) {
        if (token != null) {
            mytoken = token

            networkService.getMe { user ->
                myself = user

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        } else {
            Toast.makeText(
                this,
                getString(R.string.authentication_error_message),
                Toast.LENGTH_SHORT
            ).show()

            loginEdt.text.clear()
            passwordEdt.text.clear()
        }
    }
}
