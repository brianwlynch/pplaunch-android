package com.nepgroup.pplaunch

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

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

class SettingsRepository(context: Context) {

    private val dataStore = context.dataStore

    suspend fun getAll(): Preferences = dataStore.data.first()

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
