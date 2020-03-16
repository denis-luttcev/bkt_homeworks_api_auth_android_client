package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import android.app.Application
import android.content.Context
import io.ktor.util.KtorExperimentalAPI
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.eagerSingleton
import org.kodein.di.generic.instance
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.NetworkService
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.NetworkServiceWithKtorHttpClientImpl
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.PostRepository
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.PostRepositoryInMemoryImpl
import java.util.UUID

@KtorExperimentalAPI
val kodein = Kodein{
    bind<NetworkService>() with eagerSingleton {
        NetworkServiceWithKtorHttpClientImpl()
    }
    bind<PostRepository>() with eagerSingleton {
        PostRepositoryInMemoryImpl()
    }
}

@KtorExperimentalAPI
val networkService by kodein.instance<NetworkService>()
@KtorExperimentalAPI
val repository by kodein.instance<PostRepository>()

var mytoken: String? = null
var myself: User? = null

const val SECURITY = "security"
const val TOKEN = "token"

@KtorExperimentalAPI
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val sharedPreferences = getSharedPreferences(SECURITY, Context.MODE_PRIVATE)

        mytoken = sharedPreferences.getString(TOKEN, null)

        if (mytoken != null) {
            networkService.getMe {
                if (it != null) {

                    myself = it

                } else {

                    mytoken = null

                    sharedPreferences.edit().remove("token").apply()
                }
            }
        }
    }
}

data class User(val id: UUID, val username: String) {
    data class RegistrationRequestDto(val username: String, val login: String, val password: String)
    data class AuthenticationRequestDto(val login: String, val password: String)
    data class AuthenticationResponseDto(val token: String)
}