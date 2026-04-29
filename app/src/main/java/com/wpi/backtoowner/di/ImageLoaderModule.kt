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
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Coil uses its own OkHttp; it must use the **same** [ListenableCookieJar] as [io.appwrite.Client]
 * so Storage `/view` requests send the same session cookies (see [AppwriteModule]).
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageLoaderModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        appwriteSessionCookieJar: ListenableCookieJar,
    ): ImageLoader {
        val origin = "appwrite-android://${context.packageName}"
        val okHttp = OkHttpClient.Builder()
            .cookieJar(appwriteSessionCookieJar)
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
