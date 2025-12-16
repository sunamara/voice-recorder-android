# Voice Recorder App

A modern Android voice recording application built with Jetpack Compose and Material Design 3.

## Features

- 🎙️ **High-Quality Audio Recording** - Record audio in 3GP format
- ⏸️ **Pause & Resume** - Pause and resume recording (Android N+)
- 🎵 **Multiple Recording Modes** - Standard, Interview, and Speech-to-text modes
- 📝 **Recording Management** - View all your recordings in an organized list
- ▶️ **Playback Controls** - Play, pause, and stop recordings
- 📊 **Waveform Visualization** - Real-time audio waveform display
- 🔖 **Bookmarks** - Add bookmarks during recording
- 📱 **Material Design 3** - Modern, beautiful UI with custom theme
- 💾 **Automatic Saving** - Each recording is saved with a unique timestamp

## Screenshots

### Main Recording Screen
- Timer with millisecond precision
- Real-time waveform visualization
- Recording mode selection (Standard/Interview/Speech-to-text)
- Control buttons for record, play, pause, and stop

### Recordings List
- View all saved recordings
- Display recording name, date, and file size
- Play/stop recordings directly from the list
- Individual controls for each recording

## Technical Details

### Built With
- **Kotlin** - Primary programming language
- **Jetpack Compose** - Modern UI toolkit
- **Material Design 3** - UI components and theming
- **MediaRecorder API** - Audio recording
- **MediaPlayer API** - Audio playback
- **Coroutines** - Asynchronous operations

### Architecture
- Single Activity architecture
- Composable functions for UI
- State management with `remember` and `mutableStateOf`
- Custom vector icons for Pause, Stop, and Microphone

### Permissions Required
- `RECORD_AUDIO` - Required for recording audio

## File Structure

```
app/
├── src/main/
│   ├── java/com/example/myapplication/
│   │   └── MainActivity.kt          # Main activity with recording logic
│   └── res/
│       └── values/
│           ├── themes/
│           │   ├── Color.kt         # Color definitions
│           │   ├── Theme.kt         # App theme
│           │   └── Type.kt          # Typography
│           └── strings.xml          # String resources
```

## How It Works

### Recording
1. Each recording is saved with a unique timestamp filename
2. Files are stored in: `External Storage/Music/Recordings/`
3. Format: `Recording_YYYYMMDD_HHMMSS.3gp`

### Playback
- Recordings can be played from the main screen (latest recording)
- Or from the recordings list (any recording)
- Visual feedback shows which recording is currently playing

### Storage
- Recordings are stored in the app's external files directory
- Files persist even after app is closed
- Organized in a dedicated "Recordings" folder

## Installation

1. Clone this repository
2. Open in Android Studio
3. Build and run on an Android device or emulator
4. Grant microphone permission when prompted

## Requirements

- Android SDK 24 (Android 7.0) or higher
- Android Studio Arctic Fox or later
- Kotlin 1.9+

## Future Enhancements

- [ ] Share recordings
- [ ] Delete individual recordings
- [ ] Rename recordings
- [ ] Audio quality settings
- [ ] Cloud backup integration
- [ ] Speech-to-text functionality
- [ ] Audio editing features
- [ ] Export to different formats

## License

This project is created for personal use.


