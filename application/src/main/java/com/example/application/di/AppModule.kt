package com.example.application.di

import android.content.Context
import android.content.SharedPreferences
import com.example.application.domain.IPrinterRepository
import com.example.application.domain.PrinterRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun provideNoteRepository(): IPrinterRepository {
        return PrinterRepository()
    }

    @Singleton
    @Provides
    fun provideSharedPreference(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("SETTINGS", Context.MODE_PRIVATE)
    }
}