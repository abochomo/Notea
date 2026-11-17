package com.notea.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class AppSettings(
    /** 0 = según el sistema, 1 = claro, 2 = oscuro. */
    val themeMode: Int = 0,
    val palette: String = "indigo",
    val amoled: Boolean = false,
    val grid: Boolean = false,
    /** 0 = última modificación, 1 = fecha de creación, 2 = título. */
    val sort: Int = 0,
    val askedNotifications: Boolean = false,
)

class Prefs(private val ctx: Context) {
    private val themeKey = intPreferencesKey("theme_mode")
    private val paletteKey = stringPreferencesKey("palette")
    private val amoledKey = booleanPreferencesKey("amoled")
    private val gridKey = booleanPreferencesKey("grid")
    private val sortKey = intPreferencesKey("sort")
    private val askedKey = booleanPreferencesKey("asked_notifications")

    val flow: Flow<AppSettings> = ctx.dataStore.data.map { p ->
        AppSettings(
            themeMode = p[themeKey] ?: 0,
            palette = p[paletteKey] ?: "indigo",
            amoled = p[amoledKey] ?: false,
            grid = p[gridKey] ?: false,
            sort = p[sortKey] ?: 0,
            askedNotifications = p[askedKey] ?: false,
        )
    }

    suspend fun setThemeMode(v: Int) {
        ctx.dataStore.edit { it[themeKey] = v }
    }

    suspend fun setPalette(v: String) {
        ctx.dataStore.edit { it[paletteKey] = v }
    }

    suspend fun setAmoled(v: Boolean) {
        ctx.dataStore.edit { it[amoledKey] = v }
    }

    suspend fun setGrid(v: Boolean) {
        ctx.dataStore.edit { it[gridKey] = v }
    }

    suspend fun setSort(v: Int) {
        ctx.dataStore.edit { it[sortKey] = v }
    }

    suspend fun setAskedNotifications(v: Boolean) {
        ctx.dataStore.edit { it[askedKey] = v }
    }
}
