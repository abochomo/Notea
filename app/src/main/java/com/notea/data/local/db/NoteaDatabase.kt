package com.notea.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.notea.data.local.converter.DbConverters
import com.notea.data.local.dao.NoteDao
import com.notea.data.local.dao.ReminderDao
import com.notea.data.local.dao.RoutineDao
import com.notea.data.local.entity.AnswerEntity
import com.notea.data.local.entity.NoteEntity
import com.notea.data.local.entity.QuestionOptionEntity
import com.notea.data.local.entity.ReminderEntity
import com.notea.data.local.entity.RoutineEntity
import com.notea.data.local.entity.RoutineExecutionEntity
import com.notea.data.local.entity.RoutineQuestionEntity
import com.notea.data.local.entity.RoutineScheduleEntity

@Database(
    entities = [
        NoteEntity::class,
        ReminderEntity::class,
        RoutineEntity::class,
        RoutineScheduleEntity::class,
        RoutineQuestionEntity::class,
        QuestionOptionEntity::class,
        RoutineExecutionEntity::class,
        AnswerEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DbConverters::class)
abstract class NoteaDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun reminderDao(): ReminderDao
    abstract fun routineDao(): RoutineDao
}
