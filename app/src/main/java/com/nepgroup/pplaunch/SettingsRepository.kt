package com.nepgroup.pplaunch

import android.R.id.custom
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object SettingsKeys {
    val DEBUG = booleanPreferencesKey("debug_mode")
    val REDIRECT_MODE = stringPreferencesKey("redirect_mode")
    val URL_PREFIX = stringPreferencesKey("url_prefix")
    val TFC_INSTANCE = stringPreferencesKey("tfc_instance")
    val BASE_URL = stringPreferencesKey("base_url")
    val CUSTOM_URL = stringPreferencesKey("custom_url")
    val LOADING_STRING = stringPreferencesKey("loading_string")
    val ZOOM_PERCENT = intPreferencesKey("zoom_percent")
}

class SettingsRepository(private val context: Context) {

    private val dataStore = context.dataStore

    val debugMode: Flow<Boolean>      = dataStore.data.map { it[SettingsKeys.DEBUG] ?: false }
    val redirectMode: Flow<String>    = dataStore.data.map { it[SettingsKeys.REDIRECT_MODE] ?: "TFC" }
    val urlPrefix: Flow<String>       = dataStore.data.map { it[SettingsKeys.URL_PREFIX] ?: "CONTROL" }
    val tfcInstance: Flow<String>     = dataStore.data.map { it[SettingsKeys.TFC_INSTANCE] ?: "" }
    val baseURL: Flow<String>         = dataStore.data.map { it[SettingsKeys.BASE_URL] ?: "NEPGroup.io" }
    val customURL: Flow<String>       = dataStore.data.map { it[SettingsKeys.CUSTOM_URL] ?: "" }
    val loadingString: Flow<String>   = dataStore.data.map { it[SettingsKeys.LOADING_STRING] ?: "" }
    val zoomPercent: Flow<Int>        = dataStore.data.map { it[SettingsKeys.ZOOM_PERCENT] ?: 75}

    suspend fun getAll(): Preferences = dataStore.data.first()

    suspend fun saveAll(
        debug: Boolean,
        redirectMode: String,
        urlPrefix: String,
        baseURL: String,
        tfcInstance: String,
        customURL: String,
        loadingString: String,
        zoomPercent: Int
    ) {
        dataStore.edit { prefs ->
            prefs[SettingsKeys.DEBUG]          = debug
            prefs[SettingsKeys.REDIRECT_MODE]  = redirectMode
            prefs[SettingsKeys.URL_PREFIX]     = urlPrefix
            prefs[SettingsKeys.BASE_URL]       = baseURL
            prefs[SettingsKeys.TFC_INSTANCE]   = tfcInstance
            prefs[SettingsKeys.CUSTOM_URL]     = customURL
            prefs[SettingsKeys.LOADING_STRING] = loadingString
            prefs[SettingsKeys.ZOOM_PERCENT]   = zoomPercent
        }

    }

    fun buildURL(prefs: Preferences): String? {
        val redirectMode    = prefs[SettingsKeys.REDIRECT_MODE] ?: "TFC"
        val urlPrefix       = prefs[SettingsKeys.URL_PREFIX] ?: ""
        val baseURL         = prefs[SettingsKeys.BASE_URL] ?: ""
        val tfcInstance     = prefs[SettingsKeys.TFC_INSTANCE] ?: ""
        val customURL       = prefs[SettingsKeys.CUSTOM_URL] ?: ""

        return when(redirectMode){
            "TFC"           -> "https://$urlPrefix.$tfcInstance.$baseURL/production/pool/panel"
            "Custom URL"    -> customURL.takeIf { it.isNotBlank() }
            else -> null
        }
    }
}
