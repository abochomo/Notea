package com.notea.domain.model

enum class FrequencyType {
    DAILY, WEEKLY, MONTHLY, CUSTOM
}

enum class QuestionFieldType {
    TEXT, SINGLE_CHOICE, MULTIPLE_CHOICE, SCALE, BOOLEAN
}

enum class RoutineExecutionStatus {
    PENDING, ANSWERED, OMITTED
}

enum class HomeFilterType {
    ALL, NOTES, REMINDERS, ROUTINES
}

enum class RecentItemType {
    NOTE, REMINDER, ROUTINE
}
