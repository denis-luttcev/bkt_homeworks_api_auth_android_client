package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import io.ktor.util.KtorExperimentalAPI
import kotlinx.android.synthetic.main.activity_registration.*

@KtorExperimentalAPI
class RegistrationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        registrateBtn.setOnClickListener {
            registration()
        }
    }

    private fun registration() {
        val username = usernameEdt.text.toString()
        val login = loginEdt.text.toString()
        val password = passwordEdt.text.toString()
        val passwordConfirmation = passwordConfirmationEdt.text.toString()

        if (isCorrectInputted(login, password, username, passwordConfirmation)) {
            requestRegistration(username, login, password)
        }
    }

    private fun isCorrectInputted(
        login: String,
        password: String,
        username: String,
        passwordConfirmation: String
    )
            = validateLoginAndPassword(login, password)
            && validateUsername(username)
            && checkConfirmation(password, passwordConfirmation)

    private fun validateLoginAndPassword(login: String, password: String): Boolean {
        val condition = login.isNotEmpty() && login.isNotBlank()
                && password.isNotEmpty() && password.isNotBlank()

        if (!condition) {
            makeToast(this, R.string.authentication_data_error_message)
            clearFields()
        }

        return condition
    }

    private fun clearFields() {
        usernameEdt.text.clear()
        loginEdt.text.clear()
        passwordEdt.text.clear()
        passwordConfirmationEdt.text.clear()
    }

    private fun validateUsername(username: String): Boolean {
        val condition = username.isNotEmpty() && username.isNotBlank()

        if (!condition) {
            makeToast(this, R.string.registration_data_error_message)
            clearFields()
        }

        return condition
    }

    private fun checkConfirmation(password: String, passwordConfirmation: String): Boolean {
        val condition = password == passwordConfirmation

        if (!condition) {
            makeToast(this, R.string.password_confirmation_error_message)
            clearFields()
        }

        return condition
    }

    private fun requestRegistration(username: String, login: String, password: String) {
        sNetworkService.registrate(username, login, password, ::checkAuthentication)
    }

    private fun checkAuthentication(token: String?, message: String?) {
        if (token != null) {
            sMyToken = token
            finish()

        } else {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            clearFields()
        }
    }
}
