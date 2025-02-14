package cz.internetradio.app.api

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cz.internetradio.app.model.RadioStation
import cz.internetradio.app.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioBrowserApi @Inject constructor() {
    private val baseUrl = "https://api.radio-browser.info/json"
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun searchStationsByName(name: String): List<RadioStation>? {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/stations/byname/$name")
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            val responseBody = response.body?.string()
            val type = object : TypeToken<List<RadioStation>>() {}.type
            gson.fromJson<List<RadioStation>>(responseBody, type)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getTags(): List<Tag>? {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/tags")
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            val responseBody = response.body?.string()
            val type = object : TypeToken<List<Tag>>() {}.type
            gson.fromJson<List<Tag>>(responseBody, type)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStationsByTag(tag: String): List<RadioStation>? {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/stations/bytag/$tag")
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            val responseBody = response.body?.string()
            val type = object : TypeToken<List<RadioStation>>() {}.type
            gson.fromJson<List<RadioStation>>(responseBody, type)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStationsByCountry(country: String): List<RadioStation>? {
        return try {
            val request = Request.Builder()
                .url("$baseUrl/stations/bycountry/$country")
                .build()

            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            val responseBody = response.body?.string()
            val type = object : TypeToken<List<RadioStation>>() {}.type
            gson.fromJson<List<RadioStation>>(responseBody, type)
        } catch (e: Exception) {
            null
        }
    }
} 