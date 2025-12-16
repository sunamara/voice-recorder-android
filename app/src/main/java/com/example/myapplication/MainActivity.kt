package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random


data class Recording(
    val file: File,
    val name: String,
    val date: String,
    val size: Long
)

class MainActivity : ComponentActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: String = ""
    private lateinit var recordingsDir: File
    private val _isPlaying = mutableStateOf(false)
    private val _currentPlayingFile = mutableStateOf<String?>(null)
    val isPlaying: State<Boolean> = _isPlaying
    val currentPlayingFile: State<String?> = _currentPlayingFile

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Microphone permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recordingsDir = File(getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC), "Recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }

        Log.d("REC_APP", "Recordings directory: ${recordingsDir.absolutePath}")

        checkMicPermission()

        setContent {
            MyApplicationTheme {
                var showRecordingsList by remember { mutableStateOf(false) }
                var refreshKey by remember { mutableStateOf(0) }
                
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundLight
                ) {
                    if (showRecordingsList) {
                        key(refreshKey) {
                            RecordingsListScreen(
                                onBack = { showRecordingsList = false },
                                recordings = getAllRecordings(),
                                onPlayRecording = { filePath -> playRecording(filePath) },
                                onStopPlayback = { stopPlayback() },
                                onDeleteRecording = { file -> 
                                    val result = deleteRecording(file)
                                    if (result) {
                                        refreshKey++
                                    }
                                    result
                                },
                                isPlaying = isPlaying.value,
                                currentPlayingFile = currentPlayingFile.value
                            )
                        }
                    } else {
                        RecorderScreen(
                            onStart = { startRecording() },
                            onStop = { stopRecording() },
                            onPlay = { playRecording() },
                            onPause = { pauseRecording() },
                            onStopPlayback = { stopPlayback() },
                            onShowList = { showRecordingsList = true },
                            isPlaying = isPlaying.value,
                            hasRecordings = getAllRecordings().isNotEmpty()
                        )
                    }
                }
            }
        }
    }

    private fun checkMicPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecording() {
        if (mediaRecorder != null) return

        // Create unique filename with timestamp
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "Recording_$timestamp.3gp"
        currentRecordingFile = File(recordingsDir, fileName).absolutePath

        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            MediaRecorder()
        }

        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(currentRecordingFile)

            try {
                prepare()
                start()
                Toast.makeText(this@MainActivity, "Recording started 🎙️", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Error starting recording", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                release()
            }
        }
        mediaRecorder = null
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show()
    }

    private fun pauseRecording() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaRecorder?.pause()
            Toast.makeText(this, "Recording paused", Toast.LENGTH_SHORT).show()
        }
    }

    private fun playRecording(filePath: String? = null) {
        val fileToPlay = filePath ?: currentRecordingFile
        
        if (!File(fileToPlay).exists()) {
            Toast.makeText(this, "No recording found", Toast.LENGTH_SHORT).show()
            return
        }

        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(fileToPlay)
                prepare()
                start()
                _isPlaying.value = true
                _currentPlayingFile.value = fileToPlay
                Toast.makeText(this@MainActivity, "Playing ▶️", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Error playing audio", Toast.LENGTH_SHORT).show()
            }
        }

        mediaPlayer?.setOnCompletionListener {
            it.release()
            mediaPlayer = null
            _isPlaying.value = false
            _currentPlayingFile.value = null
        }
    }
    
    private fun getAllRecordings(): List<Recording> {
        if (!recordingsDir.exists()) return emptyList()
        
        return recordingsDir.listFiles()?.filter { it.extension == "3gp" }?.map { file ->
            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
            Recording(
                file = file,
                name = file.nameWithoutExtension.replace("Recording_", "Recording "),
                date = dateFormat.format(Date(file.lastModified())),
                size = file.length()
            )
        }?.sortedByDescending { it.file.lastModified() } ?: emptyList()
    }
    
    private fun deleteRecording(file: File): Boolean {
        return try {
            // Stop playback if this file is currently playing
            if (_currentPlayingFile.value == file.absolutePath) {
                stopPlayback()
            }
            
            val deleted = file.delete()
            if (deleted) {
                Toast.makeText(this, "Recording deleted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to delete recording", Toast.LENGTH_SHORT).show()
            }
            deleted
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error deleting recording", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun stopPlayback() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
        _isPlaying.value = false
        _currentPlayingFile.value = null
    }

    override fun onStop() {
        super.onStop()
        mediaRecorder?.release()
        mediaRecorder = null
        mediaPlayer?.release()
        mediaPlayer = null
    }
}

enum class RecordingMode {
    STANDARD, INTERVIEW, SPEECH_TO_TEXT
}

enum class RecorderState {
    IDLE, RECORDING, PAUSED, PLAYING
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecorderScreen(
    onStart: () -> Unit,
    onStop: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStopPlayback: () -> Unit,
    onShowList: () -> Unit,
    isPlaying: Boolean,
    hasRecordings: Boolean
) {
    var selectedMode by remember { mutableStateOf(RecordingMode.STANDARD) }
    var recorderState by remember { mutableStateOf(RecorderState.IDLE) }
    var hasRecording by remember { mutableStateOf(hasRecordings) }
    var elapsedTime by remember { mutableStateOf(0) }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // Update hasRecording when hasRecordings changes
    LaunchedEffect(hasRecordings) {
        hasRecording = hasRecordings
    }
    
    // Update playing state
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            recorderState = RecorderState.PLAYING
        } else if (recorderState == RecorderState.PLAYING) {
            recorderState = RecorderState.IDLE
        }
    }

    // Timer effect
    LaunchedEffect(recorderState) {
        if (recorderState == RecorderState.RECORDING) {
            while (true) {
                delay(100)
                elapsedTime += 100
            }
        } else if (recorderState == RecorderState.IDLE) {
            elapsedTime = 0
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Voice recorder", fontSize = 20.sp) },
            actions = {
                TextButton(onClick = { onShowList() }) {
                    Text("List", color = TextPrimary)
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                showMenu = false
                                Toast.makeText(context, "Settings", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                showMenu = false
                                Toast.makeText(context, "Share", Toast.LENGTH_SHORT).show()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete all") },
                            onClick = {
                                showMenu = false
                                Toast.makeText(context, "Delete all", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BackgroundLight
            )
        )

        // Recording Mode Tabs
        RecordingModeTabs(
            selectedMode = selectedMode,
            onModeSelected = { selectedMode = it },
            enabled = recorderState == RecorderState.IDLE
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Timer Display
        TimerDisplay(
            elapsedTime = elapsedTime,
            isRecording = recorderState == RecorderState.RECORDING
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Waveform Visualization
        WaveformVisualization(
            isRecording = recorderState == RecorderState.RECORDING,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        // Bookmark Button (only visible when recording)
        if (recorderState == RecorderState.RECORDING) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                BookmarkButton(onClick = {
                    Toast.makeText(context, "Bookmark added", Toast.LENGTH_SHORT).show()
                })
                Spacer(modifier = Modifier.width(24.dp))
            }
        }

        // Control Buttons
        ControlButtons(
            recorderState = recorderState,
            hasRecording = hasRecording,
            onRecordClick = {
                when (recorderState) {
                    RecorderState.IDLE -> {
                        onStart()
                        recorderState = RecorderState.RECORDING
                        hasRecording = true
                    }
                    RecorderState.RECORDING -> {
                        onStop()
                        recorderState = RecorderState.IDLE
                    }
                    else -> {}
                }
            },
            onPlayClick = {
                if (recorderState == RecorderState.PLAYING) {
                    onStopPlayback()
                    recorderState = RecorderState.IDLE
                } else if (hasRecording) {
                    onPlay()
                    recorderState = RecorderState.PLAYING
                } else {
                    Toast.makeText(context, "No recording yet", Toast.LENGTH_SHORT).show()
                }
            },
            onPauseClick = {
                when (recorderState) {
                    RecorderState.RECORDING -> {
                        onPause()
                        recorderState = RecorderState.PAUSED
                    }
                    RecorderState.PLAYING -> {
                        recorderState = RecorderState.IDLE
                    }
                    else -> {}
                }
            },
            onStopClick = {
                onStop()
                recorderState = RecorderState.IDLE
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun RecordingModeTabs(
    selectedMode: RecordingMode,
    onModeSelected: (RecordingMode) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ModeTab(
            text = "Standard",
            selected = selectedMode == RecordingMode.STANDARD,
            onClick = { onModeSelected(RecordingMode.STANDARD) },
            enabled = enabled
        )
        ModeTab(
            text = "Interview",
            selected = selectedMode == RecordingMode.INTERVIEW,
            onClick = { onModeSelected(RecordingMode.INTERVIEW) },
            enabled = enabled
        )
        ModeTab(
            text = "Speech-to-text",
            selected = selectedMode == RecordingMode.SPEECH_TO_TEXT,
            onClick = { onModeSelected(RecordingMode.SPEECH_TO_TEXT) },
            enabled = enabled
        )
    }
}

@Composable
fun RowScope.ModeTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) TextPrimary else Color.Transparent
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else TextSecondary,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun TimerDisplay(
    elapsedTime: Int,
    isRecording: Boolean
) {
    val minutes = (elapsedTime / 60000) % 60
    val seconds = (elapsedTime / 1000) % 60
    val centiseconds = (elapsedTime / 10) % 100

    Text(
        text = String.format("%02d:%02d.%02d", minutes, seconds, centiseconds),
        fontSize = 56.sp,
        fontWeight = FontWeight.Light,
        color = TextPrimary,
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
fun WaveformVisualization(
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    var waveformData by remember { mutableStateOf(List(50) { 0f }) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (true) {
                delay(50)
                waveformData = waveformData.drop(1) + Random.nextFloat()
            }
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val barWidth = width / waveformData.size
        val centerY = height / 2

        waveformData.forEachIndexed { index, amplitude ->
            val barHeight = amplitude * height * 0.8f
            val x = index * barWidth + barWidth / 2

            drawLine(
                color = WaveformColor,
                start = Offset(x, centerY - barHeight / 2),
                end = Offset(x, centerY + barHeight / 2),
                strokeWidth = barWidth * 0.6f,
                cap = StrokeCap.Round
            )
        }

        // Draw time markers
        val timeMarkers = listOf("00:00", "02:00", "04:00", "06:00", "08:00")
        timeMarkers.forEachIndexed { index, time ->
            val x = (width / (timeMarkers.size - 1)) * index
            drawLine(
                color = TextSecondary.copy(alpha = 0.3f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
        }
    }
}

// Custom Pause Icon
val PauseIcon: ImageVector
    get() {
        return ImageVector.Builder(
            name = "Pause",
            defaultWidth = 24.dp.toDp(),
            defaultHeight = 24.dp.toDp(),
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
                moveTo(6f, 4f)
                lineTo(10f, 4f)
                lineTo(10f, 20f)
                lineTo(6f, 20f)
                close()
            }
            path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
                moveTo(14f, 4f)
                lineTo(18f, 4f)
                lineTo(18f, 20f)
                lineTo(14f, 20f)
                close()
            }
        }.build()
    }

// Custom Stop Icon
val StopIcon: ImageVector
    get() {
        return ImageVector.Builder(
            name = "Stop",
            defaultWidth = 24.dp.toDp(),
            defaultHeight = 24.dp.toDp(),
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
                moveTo(6f, 6f)
                lineTo(18f, 6f)
                lineTo(18f, 18f)
                lineTo(6f, 18f)
                close()
            }
        }.build()
    }

// Custom Microphone Icon
val MicIcon: ImageVector
    get() {
        return ImageVector.Builder(
            name = "Microphone",
            defaultWidth = 24.dp.toDp(),
            defaultHeight = 24.dp.toDp(),
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
                // Microphone capsule (rounded rectangle)
                moveTo(9f, 4f)
                lineTo(15f, 4f)
                lineTo(15f, 12f)
                lineTo(9f, 12f)
                close()
                
                // Stand
                moveTo(11f, 16f)
                lineTo(13f, 16f)
                lineTo(13f, 20f)
                lineTo(11f, 20f)
                close()
                
                // Base
                moveTo(8f, 20f)
                lineTo(16f, 20f)
                lineTo(16f, 21f)
                lineTo(8f, 21f)
                close()
                
                // Left arc
                moveTo(7f, 10f)
                lineTo(7f, 12f)
                lineTo(6f, 12f)
                lineTo(6f, 10f)
                close()
                
                // Right arc
                moveTo(17f, 10f)
                lineTo(17f, 12f)
                lineTo(18f, 12f)
                lineTo(18f, 10f)
                close()
            }
        }.build()
    }

fun Dp.toDp(): Dp = this

@Composable
fun BookmarkButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = BookmarkYellow.copy(alpha = 0.2f),
        modifier = Modifier.padding(end = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Bookmark",
                tint = BookmarkYellow,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Bookmark",
                color = BookmarkYellow,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ControlButtons(
    recorderState: RecorderState,
    hasRecording: Boolean,
    onRecordClick: () -> Unit,
    onPlayClick: () -> Unit,
    onPauseClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Play Button
        if (recorderState != RecorderState.RECORDING) {
            IconButton(
                onClick = onPlayClick,
                enabled = hasRecording,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = if (recorderState == RecorderState.PLAYING) StopIcon else Icons.Default.PlayArrow,
                    contentDescription = if (recorderState == RecorderState.PLAYING) "Stop" else "Play",
                    tint = if (hasRecording) PlayGreen else TextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        // Record/Stop Button (Main)
        Surface(
            onClick = onRecordClick,
            shape = CircleShape,
            color = if (recorderState == RecorderState.RECORDING) RecordRed else RecordRedLight,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (recorderState == RecorderState.RECORDING) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.White, RoundedCornerShape(4.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }
        }

        // Pause Button
        if (recorderState == RecorderState.RECORDING) {
            IconButton(
                onClick = onPauseClick,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = PauseIcon,
                    contentDescription = "Pause",
                    tint = PauseGray,
                    modifier = Modifier.size(40.dp)
                )
            }
        } else {
            // Stop Button
            IconButton(
                onClick = onStopClick,
                enabled = hasRecording,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(
                    imageVector = StopIcon,
                    contentDescription = "Stop",
                    tint = if (hasRecording) TextPrimary else TextSecondary.copy(alpha = 0.3f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsListScreen(
    onBack: () -> Unit,
    recordings: List<Recording>,
    onPlayRecording: (String) -> Unit,
    onStopPlayback: () -> Unit,
    onDeleteRecording: (File) -> Boolean,
    isPlaying: Boolean,
    currentPlayingFile: String?
) {
    val context = LocalContext.current
    var recordingToDelete by remember { mutableStateOf<Recording?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Recordings", fontSize = 20.sp) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BackgroundLight
            )
        )
        
        if (recordings.isNotEmpty()) {
            // Recordings List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                items(recordings) { recording ->
                    val isThisPlaying = isPlaying && currentPlayingFile == recording.file.absolutePath
                    
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clickable {
                                if (isThisPlaying) {
                                    onStopPlayback()
                                } else {
                                    onPlayRecording(recording.file.absolutePath)
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Play/Stop Icon
                            Surface(
                                shape = CircleShape,
                                color = if (isThisPlaying) RecordRed else PlayGreen,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isThisPlaying) StopIcon else Icons.Default.PlayArrow,
                                        contentDescription = if (isThisPlaying) "Stop" else "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Recording Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = recording.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${recording.date} • ${formatFileSize(recording.size)}",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                            
                            // Delete Button
                            IconButton(onClick = {
                                recordingToDelete = recording
                                showDeleteDialog = true
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = RecordRed
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Empty State
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = MicIcon,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextSecondary.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No recordings yet",
                        fontSize = 18.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Start recording to see your files here",
                        fontSize = 14.sp,
                        color = TextSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Delete Confirmation Dialog
        if (showDeleteDialog && recordingToDelete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteDialog = false
                    recordingToDelete = null
                },
                title = { Text("Delete Recording?") },
                text = { 
                    Text("Are you sure you want to delete \"${recordingToDelete?.name}\"? This action cannot be undone.") 
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            recordingToDelete?.let { recording ->
                                onDeleteRecording(recording.file)
                            }
                            showDeleteDialog = false
                            recordingToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = RecordRed
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            recordingToDelete = null
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

fun formatFileSize(size: Long): String {
    return when {
        size < 1024 -> "$size B"
        size < 1024 * 1024 -> "${size / 1024} KB"
        else -> String.format("%.2f MB", size / (1024.0 * 1024.0))
    }
}
