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

        loginBtn.setOnClickListener {
            login()
        }

        registrationBtn.setOnClickListener {
            startRegistrationActivity()
        }
    }

    override fun onStart() {
        super.onStart()

        if (User.isAuthenticated()) {
            getUserAndStartMainActivity()
        }
    }

    override fun onStop() {
        super.onStop()

        if (!User.isAuthenticated()) {
            cancelRequests()
        }
    }

    private fun getUserAndStartMainActivity() {
        networkService.getMe { user ->
            if (user == null) {
                mytoken = null
                getSharedPreferences(SECURITY, MODE_PRIVATE).edit().remove(TOKEN).apply()

                return@getMe
            }

            myself = user
            startMainActivity()
            finish()
        }
    }

    private fun cancelRequests() {
        networkService.cancellation()
    }

    private fun startRegistrationActivity() {
        startActivity(Intent(this@LoginActivity, RegistrationActivity::class.java))
    }

    private fun startMainActivity() {
        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
    }

    private fun login() {
        val login = loginEdt.text.toString()
        val password = passwordEdt.text.toString()

        if (isCorrectInputted(login, password)) {
            requestToken(login, password)
        }
    }

    private fun isCorrectInputted(login: String, password: String): Boolean {
        val condition = login.isNotEmpty() && login.isNotBlank()
                && password.isNotEmpty() && password.isNotBlank()

        if (!condition) {
            makeToast(this, R.string.authentication_data_error_message)
            clearFields()
        }

        return condition
    }

    private fun clearFields() {
        loginEdt.text.clear()
        passwordEdt.text.clear()
    }

    private fun requestToken(login: String, password: String) {
        networkService.authenticate(login, password, ::checkAuthentication)
    }

    private fun checkAuthentication(token: String?) {
        if (token == null) {
            makeToast(this, R.string.authentication_error_message)
            clearFields()

            return
        }

        mytoken = token
        getSharedPreferences(SECURITY, MODE_PRIVATE).edit().putString(TOKEN, mytoken).apply()

        getUserAndStartMainActivity()
    }
}
