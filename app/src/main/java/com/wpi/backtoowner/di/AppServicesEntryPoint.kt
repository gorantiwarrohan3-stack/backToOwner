package com.wpi.backtoowner.di

import com.wpi.backtoowner.notifications.InAppNotificationStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppServicesEntryPoint {
    fun inAppNotificationStore(): InAppNotificationStore
}
