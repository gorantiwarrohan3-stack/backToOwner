package com.wpi.backtoowner.di

import android.content.Context
import coil.ImageLoader
import com.wpi.backtoowner.config.AppwriteConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.appwrite.cookies.ListenableCookieJar
import io.appwrite.cookies.stores.SharedPreferencesCookieStore
import okhttp3.OkHttpClient
import java.net.CookieManager
import java.net.CookiePolicy
import javax.inject.Singleton

private const val APPWRITE_COOKIE_PREFS = "myCookie"

/**
 * Coil uses its own OkHttp; it must share Appwrite’s cookie jar so Storage `/view` URLs
 * send the same session cookies as [io.appwrite.Client].
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(@ApplicationContext context: Context): ImageLoader {
        val cookieHandler = CookieManager(
            SharedPreferencesCookieStore(
                context.getSharedPreferences(APPWRITE_COOKIE_PREFS, Context.MODE_PRIVATE),
            ),
            CookiePolicy.ACCEPT_ALL,
        )
        val origin = "appwrite-android://${context.packageName}"
        val okHttp = OkHttpClient.Builder()
            .cookieJar(ListenableCookieJar(cookieHandler))
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("X-Appwrite-Project", AppwriteConfig.PROJECT_ID)
                    .header("Origin", origin)
                    .build()
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(okHttp)
            .respectCacheHeaders(false)
            .build()
    }
}
