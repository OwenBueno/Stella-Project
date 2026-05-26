package com.stella.feature.nfc

import androidx.lifecycle.ViewModel
import com.stella.core.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

const val NfcEnrollmentDefaultMessage = "Hold your bathroom tag to the phone"

@HiltViewModel
class NfcEnrollmentViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {
    private val _message = MutableStateFlow(NfcEnrollmentDefaultMessage)
    val message: StateFlow<String> = _message.asStateFlow()

    fun onTagRead(tagId: String) {
        settingsRepository.setNfcTagId(tagId)
        _message.update { "Tag registered." }
    }

    fun onReadFailed() {
        _message.update { "Could not read tag. Try again." }
    }

    fun registerDebugTag() {
        settingsRepository.setNfcTagId(SettingsRepository.DEBUG_NFC_TAG)
        _message.update { "Debug tag registered." }
    }
}
