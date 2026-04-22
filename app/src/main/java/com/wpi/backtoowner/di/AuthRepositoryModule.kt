package com.wpi.backtoowner.di

import com.wpi.backtoowner.data.repository.AppwriteAuthRepository
import com.wpi.backtoowner.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AuthRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AppwriteAuthRepository): AuthRepository
}
