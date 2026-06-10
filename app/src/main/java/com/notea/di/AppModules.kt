package com.notea.di

import android.content.Context
import androidx.room.Room
import com.notea.data.local.dao.NoteDao
import com.notea.data.local.dao.ReminderDao
import com.notea.data.local.dao.RoutineDao
import com.notea.data.local.db.NoteaDatabase
import com.notea.data.repository.CalendarRepositoryImpl
import com.notea.data.repository.NoteRepositoryImpl
import com.notea.data.repository.ReminderRepositoryImpl
import com.notea.data.repository.RoutineRepositoryImpl
import com.notea.data.scheduler.AlarmReminderScheduler
import com.notea.data.scheduler.ReminderScheduler
import com.notea.domain.repository.CalendarRepository
import com.notea.domain.repository.NoteRepository
import com.notea.domain.repository.ReminderRepository
import com.notea.domain.repository.RoutineRepository
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
object ProviderModule {

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NoteaDatabase =
        Room.databaseBuilder(context, NoteaDatabase::class.java, "notea.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideNoteDao(database: NoteaDatabase): NoteDao = database.noteDao()

    @Provides
    fun provideReminderDao(database: NoteaDatabase): ReminderDao = database.reminderDao()

    @Provides
    fun provideRoutineDao(database: NoteaDatabase): RoutineDao = database.routineDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindModule {

    @Binds
    abstract fun bindNoteRepository(impl: NoteRepositoryImpl): NoteRepository

    @Binds
    abstract fun bindReminderRepository(impl: ReminderRepositoryImpl): ReminderRepository

    @Binds
    abstract fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository

    @Binds
    abstract fun bindCalendarRepository(impl: CalendarRepositoryImpl): CalendarRepository

    @Binds
    abstract fun bindReminderScheduler(impl: AlarmReminderScheduler): ReminderScheduler
}
