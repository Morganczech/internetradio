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

    // Custom DNS that falls back to Google DNS-over-HTTPS (8.8.8.8) if System DNS fails
    private inner class HybridDns : okhttp3.Dns {
        override fun lookup(hostname: String): List<java.net.InetAddress> {
            try {
                // 1. Try System DNS first
                return okhttp3.Dns.SYSTEM.lookup(hostname)
            } catch (e: java.net.UnknownHostException) {
                // 2. If System DNS fails, try Google DoH via generic IP (8.8.8.8)
                try {
                    return resolveViaGoogleDoH(hostname)
                } catch (e2: Exception) {
                    
                    // 3. Fallback: If this is a radio-browser domain, try to resolve 'all.api.radio-browser.info'
                    // This works because servers use wildcard certs (*.api.radio-browser.info)
                    if (hostname.contains("radio-browser.info")) {
                        try {
                             return resolveViaGoogleDoH("all.api.radio-browser.info")
                        } catch (e3: Exception) {
                             // Critical: All DNS fallbacks failed
                        }
                    }
                    // If fallback also fails or not applicable, throw the original exception
                    throw e
                }
            }
        }
    }

    private fun resolveViaGoogleDoH(hostname: String): List<java.net.InetAddress> {
        // Construct request to Google Public DNS JSON API using raw IP
        // Using type=1 (A record) explicitly
        val url = "https://8.8.8.8/resolve?name=$hostname&type=1"
        val request = Request.Builder()
            .url(url)
            .addHeader("Accept", "application/json")
            .build()


        // We use a separate fresh client for DNS to avoid recursion loops
        val dnsClient = OkHttpClient()
        val response = dnsClient.newCall(request).execute()
        
        if (!response.isSuccessful) throw java.io.IOException("DoH HTTP Error: ${response.code}")

        val body = response.body?.string() ?: throw java.io.IOException("Empty DoH response")
        
        // Simple manual JSON parsing to avoid overhead/complexity of creating data classes just for this
        // We look for "data": "IP_ADDRESS" inside the "Answer" array
        val gson = Gson()
        val map = gson.fromJson(body, Map::class.java)
        
        @Suppress("UNCHECKED_CAST")
        val answers = map["Answer"] as? List<Map<String, Any>> 
            ?: throw java.net.UnknownHostException("No Answer section in DoH response")

        val result = mutableListOf<java.net.InetAddress>()
        for (ans in answers) {
            val type = (ans["type"] as? Number)?.toInt()
            val data = ans["data"] as? String
            // Type 1 is A record (IPv4)
            if (type == 1 && data != null) {
                result.addAll(java.net.InetAddress.getAllByName(data).toList())
            }
        }

        if (result.isEmpty()) throw java.net.UnknownHostException("No A records found via DoH for $hostname")
        
        return result
    }

    private val client = OkHttpClient.Builder()
        .dns(HybridDns()) // Use our robust DNS
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
                    .url("${baseUrls[1]}/tags")
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
                    .url("${baseUrls[1]}/stations/bytag/$encodedTag")
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
                    .url("${baseUrls[1]}/stations/bycountry/$encodedCountry")
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