package uz.jurabekov.guard.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.jurabekov.guard.core.network.ApiResult
import uz.jurabekov.guard.domain.usecase.LoginUseCase

/**
 * Login dialog ViewModel.
 *
 * MVI pattern:
 *  - state    : derived dialog state (StateFlow → Compose)
 *  - event    : foydalanuvchi harakatlari (input, click)
 *  - effect   : bir martalik (Toast, Navigation) — Channel
 *
 * Single-flight protection:
 *  - submitJob alohida saqlanadi; takroriy Submit'lar ignore qilinadi
 *    (avval canSubmit=false bo'lganda ham UI bosa olmaydi, lekin defensive).
 *
 * Lifetime: Koin viewModel — LoginDialog Composition'i o'lganda
 * onCleared() chaqiriladi va active so'rov cancel bo'ladi.
 */
class LoginViewModel(
    private val loginUseCase: LoginUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _effect = Channel<LoginUiEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    private var submitJob: Job? = null

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.UsernameChanged -> _state.update {
                // Username input'iga whitespace tushishi mumkin (autocomplete) —
                // filter qilamiz. Error darhol o'chadi — foydalanuvchi yozayotgan.
                it.copy(
                    username = event.value.filterNot { ch -> ch.isWhitespace() },
                    errorMessage = null
                )
            }

            is LoginUiEvent.PasswordChanged -> _state.update {
                it.copy(password = event.value, errorMessage = null)
            }

            LoginUiEvent.TogglePasswordVisibility -> _state.update {
                it.copy(passwordVisible = !it.passwordVisible)
            }

            LoginUiEvent.Submit -> submit()

            LoginUiEvent.TelegramLoginClicked -> {
                viewModelScope.launch {
                    _effect.send(LoginUiEffect.ShowToast(MSG_IN_PROGRESS))
                }
            }

            LoginUiEvent.DialogDismissed -> {
                // Dialog yopildi → formani tozalaymiz (xavfsizlik). Yangi
                // ochilganda parol bo'sh bo'lishi shart. Active submit
                // ham cancel qilinadi.
                submitJob?.cancel()
                _state.value = LoginUiState()
            }
        }
    }

    private fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        if (submitJob?.isActive == true) return  // single-flight

        submitJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = loginUseCase(
                username = current.username,
                password = current.password
            )) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isLoading = false) }
                    _effect.send(LoginUiEffect.LoginSuccess)
                }

                is ApiResult.Error -> _state.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }

                ApiResult.NetworkError -> _state.update {
                    it.copy(isLoading = false, errorMessage = MSG_NO_INTERNET)
                }

                ApiResult.Unauthorized -> _state.update {
                    // Bu yo'l odatda ham yetib kelmaydi (401 Error'ga
                    // map bo'lgan), lekin defensive.
                    it.copy(isLoading = false, errorMessage = MSG_INVALID_CREDENTIALS)
                }
            }
        }
    }

    private companion object {
        const val MSG_IN_PROGRESS = "Jarayonda"
        const val MSG_NO_INTERNET = "Internet aloqasi yo'q"
        const val MSG_INVALID_CREDENTIALS = "Login yoki parol noto'g'ri"
    }
}
