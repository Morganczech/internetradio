package cz.internetradio.app.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.*
import com.google.android.gms.wearable.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity(), DataClient.OnDataChangedListener {
    private var radioName by mutableStateOf("")
    private var isPlaying by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            WearApp(
                radioName = radioName,
                isPlaying = isPlaying,
                onPlayPauseClick = { sendCommand("play_pause") },
                onNextClick = { sendCommand("next") },
                onPreviousClick = { sendCommand("previous") }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getDataClient(this).removeListener(this)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                when (dataItem.uri.path) {
                    "/radio_state" -> {
                        val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                        radioName = dataMap.getString("radio_name", "")
                        isPlaying = dataMap.getBoolean("is_playing", false)
                    }
                }
            }
        }
    }

    private fun sendCommand(action: String) {
        val request = PutDataMapRequest.create("/command").apply {
            dataMap.putString("action", action)
        }.asPutDataRequest()
        
        Wearable.getDataClient(this).putDataItem(request)
    }
}

@Composable
fun WearApp(
    radioName: String,
    isPlaying: Boolean,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit
) {
    val scalingLazyListState = rememberScalingLazyListState()

    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = scalingLazyListState,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = radioName.ifEmpty { "Žádné rádio nehraje" },
                style = MaterialTheme.typography.title3
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onPreviousClick,
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
                ) {
                    Text("⏮")
                }

                Button(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(ButtonDefaults.LargeButtonSize)
                ) {
                    Text(if (isPlaying) "⏸" else "▶")
                }

                Button(
                    onClick = onNextClick,
                    modifier = Modifier.size(ButtonDefaults.SmallButtonSize)
                ) {
                    Text("⏭")
                }
            }
        }
    }
} 