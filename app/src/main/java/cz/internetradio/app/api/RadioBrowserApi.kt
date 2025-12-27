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
import android.util.Log

data class SearchParams(
    val name: String,
    val country: String? = null,
    val minBitrate: Int? = null,
    val orderBy: String = "votes", // votes, name, bitrate, clickcount
    val reverse: Boolean = true,
    val limit: Int = 100,
    val hideBroken: Boolean = true
)

@Singleton
class RadioBrowserApi @Inject constructor() {
    private val baseUrls = listOf(
        "https://at1.api.radio-browser.info/json",
        "https://de1.api.radio-browser.info/json",
        "https://nl1.api.radio-browser.info/json",
        "https://fr1.api.radio-browser.info/json",
        "https://us1.api.radio-browser.info/json",
        "https://es1.api.radio-browser.info/json"
    )
    private val baseUrl get() = baseUrls[1]
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun searchStations(params: SearchParams): List<RadioStation>? {
        return withContext(Dispatchers.IO) {
            val queryParams = mutableListOf<String>()
            
            // Přidání parametrů do URL
            if (params.name.isNotBlank()) {
                queryParams.add("name=${java.net.URLEncoder.encode(params.name.trim(), "UTF-8").replace("+", "%20")}")
            }
            params.country?.let { 
                if (it.length == 2) {
                    queryParams.add("countrycode=${java.net.URLEncoder.encode(it, "UTF-8")}")
                } else {
                    queryParams.add("country=${java.net.URLEncoder.encode(it, "UTF-8")}")
                }
            }
            params.minBitrate?.let {
                queryParams.add("minBitrate=$it")
            }
            queryParams.add("order=${params.orderBy}")
            queryParams.add("reverse=${params.reverse}")
            queryParams.add("limit=${params.limit}")
            queryParams.add("hidebroken=${params.hideBroken}")

            val queryString = queryParams.joinToString("&")
            
            for (baseUrl in baseUrls) {
                try {
                    val url = "$baseUrl/stations/search?$queryString"
                    Log.d("RadioBrowserApi", "Volám API: $url")

                    val request = Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "InternetRadio/1.0")
                        .build()

                    val response = client.newCall(request).execute()
                    try {
                        if (response.isSuccessful) {
                            val json = response.body?.string()
                            if (json != null) {
                                val listType = object : TypeToken<List<RadioStation>>() {}.type
                                val stations = gson.fromJson<List<RadioStation>>(json, listType)
                                Log.d("RadioBrowserApi", "Počet nalezených stanic: ${stations?.size ?: 0}")
                                return@withContext stations
                            }
                        } else {
                            Log.e("RadioBrowserApi", "API Error: ${response.code} on $baseUrl")
                        }
                    } finally {
                        response.close()
                    }
                } catch (e: Exception) {
                    Log.e("RadioBrowserApi", "Chyba při komunikaci se serverem $baseUrl: ${e.message}")
                    // Pokračujeme na další server
                }
            }
            Log.e("RadioBrowserApi", "Všechny servery selhaly")
            null
        }
    }

    suspend fun getTags(): List<Tag>? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$baseUrl/tags")
                    .addHeader("User-Agent", "InternetRadio/1.0")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext null
                }

                val responseBody = response.body?.string()
                val type = object : TypeToken<List<Tag>>() {}.type
                gson.fromJson<List<Tag>>(responseBody, type)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getStationsByTag(tag: String): List<RadioStation>? {
        return withContext(Dispatchers.IO) {
            try {
                val encodedTag = java.net.URLEncoder.encode(tag, "UTF-8")
                val request = Request.Builder()
                    .url("$baseUrl/stations/bytag/$encodedTag")
                    .addHeader("User-Agent", "InternetRadio/1.0")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext null
                }

                val responseBody = response.body?.string()
                val type = object : TypeToken<List<RadioStation>>() {}.type
                gson.fromJson<List<RadioStation>>(responseBody, type)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getStationsByCountry(country: String): List<RadioStation>? {
        return withContext(Dispatchers.IO) {
            try {
                val encodedCountry = java.net.URLEncoder.encode(country, "UTF-8")
                val request = Request.Builder()
                    .url("$baseUrl/stations/bycountry/$encodedCountry")
                    .addHeader("User-Agent", "InternetRadio/1.0")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext null
                }

                val responseBody = response.body?.string()
                val type = object : TypeToken<List<RadioStation>>() {}.type
                gson.fromJson<List<RadioStation>>(responseBody, type)
            } catch (e: Exception) {
                null
            }
        }
    }
} 