package com.wpi.backtoowner.di

import com.wpi.backtoowner.data.mlkit.TfliteImageLabelingAnalyzer
import com.wpi.backtoowner.domain.analysis.ImageLabelingAnalyzer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ImageLabelingAnalyzerModule {

    @Binds
    @Singleton
    abstract fun bindImageLabelingAnalyzer(impl: TfliteImageLabelingAnalyzer): ImageLabelingAnalyzer
}
