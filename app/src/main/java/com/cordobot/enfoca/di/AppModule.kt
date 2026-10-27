package com.cordobot.enfoca.di

import android.content.Context
import androidx.room.Room
import com.cordobot.enfoca.data.local.HabitDao
import com.cordobot.enfoca.data.local.HabitDatabase
import com.cordobot.enfoca.data.reminder.AndroidReminderScheduler
import com.cordobot.enfoca.data.repository.HabitRepositoryImpl
import com.cordobot.enfoca.data.repository.PomodoroStore
import com.cordobot.enfoca.domain.repository.HabitRepository
import com.cordobot.enfoca.domain.repository.PomodoroRepository
import com.cordobot.enfoca.domain.repository.ReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HabitDatabase =
        Room.databaseBuilder(context, HabitDatabase::class.java, "enfoca.db").build()

    @Provides
    fun provideHabitDao(database: HabitDatabase): HabitDao = database.habitDao()

    // Injected so tests can fix "today"
    @Provides
    fun provideClock(): Clock = Clock.systemDefaultZone()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {
    @Binds abstract fun bindHabitRepository(impl: HabitRepositoryImpl): HabitRepository
    @Binds abstract fun bindPomodoroRepository(impl: PomodoroStore): PomodoroRepository
    @Binds abstract fun bindReminderScheduler(impl: AndroidReminderScheduler): ReminderScheduler
}
