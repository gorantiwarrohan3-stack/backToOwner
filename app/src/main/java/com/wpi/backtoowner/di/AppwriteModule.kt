package com.wpi.backtoowner.di

import android.content.Context
import com.wpi.backtoowner.config.AppwriteConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.appwrite.Client
import io.appwrite.cookies.ListenableCookieJar
import io.appwrite.cookies.stores.SharedPreferencesCookieStore
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import java.net.CookieManager
import java.net.CookiePolicy
import javax.inject.Singleton

/** Must match [io.appwrite.Client] companion `COOKIE_PREFS` (`"myCookie"`). */
private const val APPWRITE_COOKIE_PREFS = "myCookie"

@Module
@InstallIn(SingletonComponent::class)
object AppwriteModule {

    /**
     * Single cookie jar for Appwrite HTTP and Coil. Two [SharedPreferencesCookieStore] instances
     * would share the same prefs file but **not** the same in-memory index, so session cookies
     * saved by the SDK would not be visible to image requests until process restart.
     */
    @Provides
    @Singleton
    fun provideAppwriteSessionCookieJar(@ApplicationContext context: Context): ListenableCookieJar {
        val handler = CookieManager(
            SharedPreferencesCookieStore(
                context.getSharedPreferences(APPWRITE_COOKIE_PREFS, Context.MODE_PRIVATE),
            ),
            CookiePolicy.ACCEPT_ALL,
        )
        return ListenableCookieJar(handler)
    }

    @Provides
    @Singleton
    fun provideAppwriteClient(
        @ApplicationContext context: Context,
        appwriteSessionCookieJar: ListenableCookieJar,
    ): Client {
        val client = Client(context)
            .setEndpoint(AppwriteConfig.ENDPOINT)
            .setProject(AppwriteConfig.PROJECT_ID)
        wireClientToSharedCookieJar(client, appwriteSessionCookieJar)
        return client
    }

    @Provides
    @Singleton
    fun provideAccount(client: Client): Account = Account(client)

    @Provides
    @Singleton
    fun provideDatabases(client: Client): Databases = Databases(client)

    @Provides
    @Singleton
    fun provideStorage(client: Client): Storage = Storage(client)

    /**
     * [Client] constructs its own [ListenableCookieJar] in the constructor; replace it with our
     * singleton and rebuild OkHttp ([Client.setSelfSigned]) so API calls and Coil share cookies.
     */
    private fun wireClientToSharedCookieJar(client: Client, jar: ListenableCookieJar) {
        runCatching {
            val f = Client::class.java.getDeclaredField("cookieJar")
            f.isAccessible = true
            f.set(client, jar)
            client.setSelfSigned(false)
        }.getOrElse { e ->
            throw IllegalStateException(
                "Could not attach shared Appwrite cookie jar (SDK field layout may have changed).",
                e,
            )
        }
    }
}
