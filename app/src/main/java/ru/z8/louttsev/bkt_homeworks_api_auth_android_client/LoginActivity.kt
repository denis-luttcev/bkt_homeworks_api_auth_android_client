package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_login.*

@KtorExperimentalAPI
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        loginBtn.setOnClickListener(::login)

        registrationBtn.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegistrationActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()

        if (mytoken != null) {
            getUserAndStartMainActivity()
        }
    }

    override fun onStop() {
        super.onStop()

        if (mytoken == null) {
            networkService.cancellation()
        }
    }

    private fun login(view: View) {
        val login = loginEdt.text.toString()
        val password = passwordEdt.text.toString()

        if (login.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.authentication_data_error_message),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        networkService.authenticate(login, password, ::checkAuthentication)
    }

    private fun checkAuthentication(token: String?) {
        if (token == null) {
            Toast.makeText(
                this,
                getString(R.string.authentication_error_message),
                Toast.LENGTH_SHORT
            ).show()

            loginEdt.text.clear()
            passwordEdt.text.clear()

            return
        }

        mytoken = token
        getSharedPreferences(SECURITY, Context.MODE_PRIVATE).edit().putString(TOKEN, mytoken).apply()

        getUserAndStartMainActivity()
    }

    private fun getUserAndStartMainActivity() {
        networkService.getMe { user ->
            myself = user // nullable ignored

            startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            finish()
        }
    }
}
