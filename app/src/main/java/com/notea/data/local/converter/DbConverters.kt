package com.notea.data.local.converter

import androidx.room.TypeConverter
import com.notea.domain.model.FrequencyType
import com.notea.domain.model.QuestionFieldType
import com.notea.domain.model.RoutineExecutionStatus

class DbConverters {

    @TypeConverter
    fun fromFrequencyType(value: FrequencyType?): String? = value?.name

    @TypeConverter
    fun toFrequencyType(value: String?): FrequencyType? = value?.let(FrequencyType::valueOf)

    @TypeConverter
    fun fromQuestionFieldType(value: QuestionFieldType?): String? = value?.name

    @TypeConverter
    fun toQuestionFieldType(value: String?): QuestionFieldType? = value?.let(QuestionFieldType::valueOf)

    @TypeConverter
    fun fromRoutineExecutionStatus(value: RoutineExecutionStatus?): String? = value?.name

    @TypeConverter
    fun toRoutineExecutionStatus(value: String?): RoutineExecutionStatus? =
        value?.let(RoutineExecutionStatus::valueOf)
}
