package com.soundpilot.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.soundpilot.app.domain.model.SortMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.soundPilotDataStore by preferencesDataStore(name = "soundpilot_prefs")

@Serializable
data class EqualizerSettings(
    val enabled: Boolean = false,
    val profileId: String = "balanced",
    val gainsDb: List<Float> = emptyList(),
    val bassStrength: Int = 0,
    val virtualizerStrength: Int = 0,
    val bandCount: Int = 0,
)

@Serializable
data class LastPlayed(
    val songIds: List<Long> = emptyList(),
    val index: Int = 0,
    val positionMs: Long = 0,
    val shuffle: Boolean = false,
    val repeatMode: Int = 0,
    val savedAt: Long = 0,
) {
    val isEmpty: Boolean get() = songIds.isEmpty()
}

enum class ThemeMode(val id: String) {
    DARK("dark"),
    LIGHT("light"),
    SYSTEM("system"),
    ;

    companion object {
        fun fromId(id: String?): ThemeMode = entries.firstOrNull { it.id == id } ?: DARK
    }
}

/**
 * ذخیره‌سازی تنظیمات، علاقه‌مندی‌ها و آخرین وضعیت پخش با DataStore.
 * همه داده‌ها محلی می‌مانند؛ هیچ همگام‌سازی یا ارسال داده‌ای وجود ندارد.
 */
class SettingsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val EQ = stringPreferencesKey("eq_settings")
        val LAST_PLAYED = stringPreferencesKey("last_played")
        val FAVORITES = stringPreferencesKey("favorites")
        val THEME = stringPreferencesKey("theme_mode")
        val CAR_MODE = booleanPreferencesKey("car_mode")
        val SORT_MODE = stringPreferencesKey("sort_mode")
        val RESTORE_LAST = booleanPreferencesKey("restore_last_played")
    }

    private inline fun <reified T> decode(raw: String?, default: T): T =
        raw?.let { value ->
            runCatching { json.decodeFromString<T>(value) }.getOrNull()
        } ?: default

    private fun flowOf(block: (androidx.datastore.preferences.core.Preferences) -> Unit): Flow<Unit> =
        context.soundPilotDataStore.data.catch { emit(emptyPreferences()) }.map { block(it) }

    // ---------- اکولایزر ----------

    val equalizerSettings: Flow<EqualizerSettings> =
        context.soundPilotDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { decode(it[Keys.EQ], EqualizerSettings()) }

    suspend fun saveEqualizer(settings: EqualizerSettings) {
        context.soundPilotDataStore.edit { prefs ->
            prefs[Keys.EQ] = json.encodeToString(settings)
        }
    }

    // ---------- آخرین پخش ----------

    val lastPlayed: Flow<LastPlayed?> =
        context.soundPilotDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { prefs ->
                val value = decode(prefs[Keys.LAST_PLAYED], LastPlayed())
                if (value.isEmpty) null else value
            }

    suspend fun saveLastPlayed(value: LastPlayed) {
        context.soundPilotDataStore.edit { prefs ->
            prefs[Keys.LAST_PLAYED] = json.encodeToString(value)
        }
    }

    suspend fun clearLastPlayed() {
        context.soundPilotDataStore.edit { prefs -> prefs.remove(Keys.LAST_PLAYED) }
    }

    val restoreLastPlayed: Flow<Boolean> =
        context.soundPilotDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[Keys.RESTORE_LAST] ?: true }

    suspend fun setRestoreLastPlayed(value: Boolean) {
        context.soundPilotDataStore.edit { prefs -> prefs[Keys.RESTORE_LAST] = value }
    }

    // ---------- علاقه‌مندی‌ها ----------

    val favorites: Flow<Set<Long>> =
        context.soundPilotDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { decode<List<Long>>(it[Keys.FAVORITES], emptyList()).toSet() }

    /** وضعیت جدید را برمی‌گرداند: true یعنی اضافه شد */
    suspend fun toggleFavorite(songId: Long): Boolean {
        var added = false
        context.soundPilotDataStore.edit { prefs ->
            val current = decode<List<Long>>(prefs[Keys.FAVORITES], emptyList()).toMutableSet()
            if (songId in current) {
                current.remove(songId)
                added = false
            } else {
                current.add(songId)
                added = true
            }
            prefs[Keys.FAVORITES] = json.encodeToString(current.toList())
        }
        return added
    }

    // ---------- ترجیحات برنامه ----------

    val themeMode: Flow<ThemeMode> =
        context.soundPilotDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { ThemeMode.fromId(it[Keys.THEME]) }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.soundPilotDataStore.edit { prefs -> prefs[Keys.THEME] = mode.id }
    }

    val carMode: Flow<Boolean> =
        context.soundPilotDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { it[Keys.CAR_MODE] ?: false }

    suspend fun setCarMode(value: Boolean) {
        context.soundPilotDataStore.edit { prefs -> prefs[Keys.CAR_MODE] = value }
    }

    val sortMode: Flow<SortMode> =
        context.soundPilotDataStore.data
            .catch { emit(emptyPreferences()) }
            .map { SortMode.fromId(it[Keys.SORT_MODE]) }

    suspend fun setSortMode(mode: SortMode) {
        context.soundPilotDataStore.edit { prefs -> prefs[Keys.SORT_MODE] = mode.id }
    }
}
