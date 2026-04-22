package com.wpi.backtoowner.data.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Optional address or building note from the Map tab, applied when the user files a **Found** report.
 */
@Singleton
class FoundLocationDraft @Inject constructor() {

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()

    fun setAddress(value: String) {
        _address.value = value
    }

    fun clear() {
        _address.value = ""
    }

    fun current(): String = _address.value.trim()
}
