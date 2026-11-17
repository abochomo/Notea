package com.notea.app.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.notea.app.data.AppSettings

// Temporal: pantallas en construcción (se sustituyen en el siguiente commit).
@Composable fun RoutineEditorScreen(routineId: Long, day: Long, onBack: () -> Unit, onCreated: (Long) -> Unit) { Text("…") }
@Composable fun RoutineDetailScreen(routineId: Long, nav: NavController) { Text("…") }
@Composable fun EntryScreen(routineId: Long, day: Long, onBack: () -> Unit) { Text("…") }
@Composable fun SettingsScreen(settings: AppSettings, bottomBar: @Composable () -> Unit) { Text("…") }
