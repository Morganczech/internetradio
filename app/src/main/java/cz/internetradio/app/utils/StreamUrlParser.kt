package cz.internetradio.app.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

class StreamUrlParser {
    sealed class Result {
        data class Success(val streamUrl: String) : Result()
        data class Error(val message: String) : Result()
    }

    companion object {
        suspend fun parseUrl(url: String): Result = withContext(Dispatchers.IO) {
            try {
                when {
                    url.endsWith(".pls", ignoreCase = true) -> parsePls(url)
                    url.endsWith(".m3u", ignoreCase = true) -> parseM3u(url)
                    url.endsWith(".m3u8", ignoreCase = true) -> parseM3u(url)
                    else -> Result.Success(url) // Předpokládáme přímý stream
                }
            } catch (e: Exception) {
                Result.Error("Nepodařilo se načíst stream: ${e.message}")
            }
        }

        private fun parsePls(url: String): Result {
            val content = URL(url).openStream().use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }

            // Hledáme první File1= záznam
            val streamUrl = content.lines()
                .find { it.startsWith("File1=", ignoreCase = true) }
                ?.substringAfter("File1=")
                ?.trim()

            return if (streamUrl != null) {
                Result.Success(streamUrl)
            } else {
                Result.Error("Nepodařilo se najít URL streamu v PLS souboru")
            }
        }

        private fun parseM3u(url: String): Result {
            val content = URL(url).openStream().use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }

            // Hledáme první neprázdný řádek, který není komentář a začíná http/https
            val streamUrl = content.lines()
                .filter { it.isNotBlank() }
                .filter { !it.startsWith("#") }
                .find { it.startsWith("http", ignoreCase = true) }
                ?.trim()

            return if (streamUrl != null) {
                Result.Success(streamUrl)
            } else {
                Result.Error("Nepodařilo se najít URL streamu v M3U souboru")
            }
        }
    }
} 