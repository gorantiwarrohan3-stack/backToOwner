package com.wpi.backtoowner.di

import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MlKitModule {

    @Provides
    @Singleton
    fun provideImageLabeler(): ImageLabeler {
        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.52f)
            .build()
        return ImageLabeling.getClient(options)
    }
}
