package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_registration.*
import kotlinx.android.synthetic.main.activity_registration.loginEdt
import kotlinx.android.synthetic.main.activity_registration.passwordEdt

@KtorExperimentalAPI
class RegistrationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        registrateBtn.setOnClickListener(::registration)
    }

    private fun registration(view: View) {
        val username = usernameEdt.text.toString()
        val login = loginEdt.text.toString()
        val password = passwordEdt.text.toString()
        val passwordConfirmation = passwordConfirmationEdt.text.toString()

        if (login.isEmpty() || password.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.authentication_data_error_message),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (username.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.registration_data_error_message),
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (password != passwordConfirmation) {
            Toast.makeText(
                this,
                getString(R.string.password_confirmation_error_message),
                Toast.LENGTH_SHORT
            ).show()

            passwordEdt.text.clear()
            passwordConfirmationEdt.text.clear()

            return
        }

        networkService.registrate(username, login, password, ::checkAuthentication)
    }

    private fun checkAuthentication(token: String?, message: String?) {
        if (token == null) {
            Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
            ).show()

            usernameEdt.text.clear()
            loginEdt.text.clear()
            passwordEdt.text.clear()
            passwordConfirmationEdt.text.clear()

            return
        }

        mytoken = token

        networkService.getMe { user ->
            myself = user // nullable ignored

            finish()
        }
    }
}
