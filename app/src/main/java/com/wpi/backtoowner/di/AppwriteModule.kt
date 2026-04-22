package com.wpi.backtoowner.di

import android.content.Context
import com.wpi.backtoowner.config.AppwriteConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.appwrite.Client
import io.appwrite.services.Account
import io.appwrite.services.Databases
import io.appwrite.services.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppwriteModule {

    @Provides
    @Singleton
    fun provideAppwriteClient(
        @ApplicationContext context: Context,
    ): Client = Client(context)
        .setEndpoint(AppwriteConfig.ENDPOINT)
        .setProject(AppwriteConfig.PROJECT_ID)

    @Provides
    @Singleton
    fun provideAccount(client: Client): Account = Account(client)

    @Provides
    @Singleton
    fun provideDatabases(client: Client): Databases = Databases(client)

    @Provides
    @Singleton
    fun provideStorage(client: Client): Storage = Storage(client)
}
