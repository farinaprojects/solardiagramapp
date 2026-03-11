package br.com.solardiagram.util

sealed class AppResult<out T> {
    data class Ok<T>(val value: T): AppResult<T>()
    data class Err(val error: Throwable): AppResult<Nothing>()
}
