package cz.internetradio.app.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val geocoder = Geocoder(context, Locale.getDefault())

    suspend fun getCurrentCountry(): String? {
        if (!hasLocationPermission()) {
            return null
        }

        return try {
            val location = getCurrentLocation()
            location?.let { getCountryFromLocation(it) }
        } catch (e: Exception) {
            null
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_LOW_POWER,
                object : CancellationToken() {
                    private val tokenSource = CancellationTokenSource()
                    override fun onCanceledRequested(listener: OnTokenCanceledListener) =
                        tokenSource.token

                    override fun isCancellationRequested() = false
                }
            ).addOnSuccessListener { location ->
                continuation.resume(location)
            }.addOnFailureListener {
                continuation.resume(null)
            }
        } catch (e: SecurityException) {
            continuation.resume(null)
        }
    }

    private suspend fun getCountryFromLocation(location: Location): String? =
        suspendCancellableCoroutine { continuation ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    ) { addresses ->
                        if (addresses.isNotEmpty()) {
                            continuation.resume(addresses[0].countryCode)
                        } else {
                            continuation.resume(null)
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(
                        location.latitude,
                        location.longitude,
                        1
                    )
                    if (!addresses.isNullOrEmpty()) {
                        continuation.resume(addresses[0].countryCode)
                    } else {
                        continuation.resume(null)
                    }
                }
            } catch (e: Exception) {
                continuation.resume(null)
            }
        }
} 