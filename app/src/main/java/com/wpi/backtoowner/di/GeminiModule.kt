package com.wpi.backtoowner.di

import com.google.genai.Client
import com.wpi.backtoowner.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object GeminiModule {

    @Provides
    @Singleton
    fun provideGeminiClient(): Client {
        return Client.builder()
            .apiKey(BuildConfig.GEMINI_API_KEY)
            .build()
    }
}
