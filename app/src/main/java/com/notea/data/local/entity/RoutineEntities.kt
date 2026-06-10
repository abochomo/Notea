package com.notea.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.notea.domain.model.FrequencyType
import com.notea.domain.model.QuestionFieldType
import com.notea.domain.model.RoutineExecutionStatus

@Entity(
    tableName = "routines",
    indices = [
        Index(value = ["name"]),
        Index(value = ["frequencyType"]),
        Index(value = ["isPaused"])
    ]
)
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true)
    val routineId: Long = 0,
    val name: String,
    val description: String?,
    val frequencyType: FrequencyType,
    val customRule: String?,
    val isPaused: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "routine_schedules",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["routineId"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routineId"]),
        Index(value = ["routineId", "timeOfDay"], unique = true)
    ]
)
data class RoutineScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    val scheduleId: Long = 0,
    val routineId: Long,
    val timeOfDay: Int,
    val isEnabled: Boolean = true
)

@Entity(
    tableName = "routine_questions",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["routineId"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routineId"]),
        Index(value = ["routineId", "displayOrder"], unique = true)
    ]
)
data class RoutineQuestionEntity(
    @PrimaryKey(autoGenerate = true)
    val questionId: Long = 0,
    val routineId: Long,
    val label: String,
    val fieldType: QuestionFieldType,
    val isRequired: Boolean = false,
    val displayOrder: Int
)

@Entity(
    tableName = "question_options",
    foreignKeys = [
        ForeignKey(
            entity = RoutineQuestionEntity::class,
            parentColumns = ["questionId"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["questionId"]),
        Index(value = ["questionId", "displayOrder"], unique = true)
    ]
)
data class QuestionOptionEntity(
    @PrimaryKey(autoGenerate = true)
    val optionId: Long = 0,
    val questionId: Long,
    val optionText: String,
    val displayOrder: Int
)

@Entity(
    tableName = "routine_executions",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["routineId"],
            childColumns = ["routineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routineId"]),
        Index(value = ["scheduledDate"]),
        Index(value = ["status"]),
        Index(value = ["routineId", "scheduledDate"], unique = true)
    ]
)
data class RoutineExecutionEntity(
    @PrimaryKey(autoGenerate = true)
    val executionId: Long = 0,
    val routineId: Long,
    val scheduledDate: Long,
    val openedAt: Long? = null,
    val answeredAt: Long? = null,
    val status: RoutineExecutionStatus = RoutineExecutionStatus.PENDING
)

@Entity(
    tableName = "answers",
    foreignKeys = [
        ForeignKey(
            entity = RoutineExecutionEntity::class,
            parentColumns = ["executionId"],
            childColumns = ["executionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RoutineQuestionEntity::class,
            parentColumns = ["questionId"],
            childColumns = ["questionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["executionId"]),
        Index(value = ["questionId"]),
        Index(value = ["executionId", "questionId"], unique = true)
    ]
)
data class AnswerEntity(
    @PrimaryKey(autoGenerate = true)
    val answerId: Long = 0,
    val executionId: Long,
    val questionId: Long,
    val valueText: String? = null,
    val valueNumber: Double? = null,
    val valueBoolean: Boolean? = null,
    val valueJson: String? = null
)
