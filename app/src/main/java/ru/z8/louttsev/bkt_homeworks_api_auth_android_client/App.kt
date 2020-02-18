package ru.z8.louttsev.bkt_homeworks_api_auth_android_client

import android.app.Application
import android.content.res.Resources
import io.ktor.util.KtorExperimentalAPI
import org.kodein.di.Kodein
import org.kodein.di.generic.bind
import org.kodein.di.generic.eagerSingleton
import org.kodein.di.generic.instance
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.NetworkService
import ru.z8.louttsev.bkt_homeworks_api_auth_android_client.services.NetworkServiceWithKtorHttpClientImpl

@KtorExperimentalAPI
val kodein = Kodein{
    bind<NetworkService>() with eagerSingleton {
        NetworkServiceWithKtorHttpClientImpl()
    }
    bind<PostAdapter>() with eagerSingleton {
        PostAdapter()
    }
}

@KtorExperimentalAPI
val networkService by kodein.instance<NetworkService>()
@KtorExperimentalAPI
val postAdapter by kodein.instance<PostAdapter>()

lateinit var sResources: Resources

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        sResources = resources
    }
}