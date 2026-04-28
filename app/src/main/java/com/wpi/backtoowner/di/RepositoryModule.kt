package com.wpi.backtoowner.di

import com.wpi.backtoowner.data.repository.AppwritePostImageRepository
import com.wpi.backtoowner.data.repository.AppwritePostRepository
import com.wpi.backtoowner.data.repository.GeminiAiMatchingRepository
import com.wpi.backtoowner.domain.repository.AiMatchingRepository
import com.wpi.backtoowner.domain.repository.PostImageRepository
import com.wpi.backtoowner.domain.repository.PostRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPostRepository(impl: AppwritePostRepository): PostRepository

    @Binds
    @Singleton
    abstract fun bindPostImageRepository(impl: AppwritePostImageRepository): PostImageRepository

    @Binds
    @Singleton
    abstract fun bindAiMatchingRepository(impl: GeminiAiMatchingRepository): AiMatchingRepository
}
