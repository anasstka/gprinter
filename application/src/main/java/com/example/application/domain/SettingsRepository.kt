package com.example.application.domain

import android.content.Context
import android.content.SharedPreferences
import android.util.Size
import javax.inject.Inject

interface ISettingsRepository {

    fun getLabelSize(): Size

    fun setLabelSize(size: Size)
}

class SettingsRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : ISettingsRepository {

    companion object {
        private const val WIDTH = "WIDTH"
        private const val HEIGHT = "HEIGHT"
    }

    override fun getLabelSize(): Size {
        val width = sharedPreferences.getInt(WIDTH, 43)
        val height = sharedPreferences.getInt(HEIGHT, 25)
        return Size(width, height)
    }

    override fun setLabelSize(size: Size) = with(sharedPreferences.edit()) {
        putInt(WIDTH, size.width)
        putInt(HEIGHT, size.height)

        apply()
    }
}