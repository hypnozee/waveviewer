# ParadoxWaveViewer

This Application aims to read .wav audio files from storage and display their waveform.
It will do so in a discrete signal processing manner.
This means the signal will be sampled a number of times.
The amplitudes will then be plotted as a visual chart.
Supports Dark Mode! Try it :)

# Code Structure

It follows a modular approach, respecting CLEAN Architecture principles and MVI pattern.

1.  **`:domain`:**
    *   Contains the core business logic and use cases.
    *   Manages audio track information.
    *   Handles audio playback operations.
    *   Provides waveform data.
2.  **`:data`:**
    *   Handles data repositories and models.
    *   Provides concrete implementations of repositories defines in `:domain`.
    *   Manages data access to audio files and related information.

3.  **`:ui`:**
    *   Implements the user interface and presentation logic.
    *   Displays audio waveforms.
    *   Respects -> UDF -> principles. It fires and it forgets.
    *   Uses Jetpack Compose.
    *   Has a UI state which stores information about all interactable components.
    *   All possible actions (intents) are gracefully handles by ViewModel
    *   `WaveScreenIntent` defines all possible intents using a sealed class.


# Dependency Injection

It uses Koin for dependency injection.

1.   **`app`**
     *   In `MainApplication` class, we start Koin and tell it about our main DI modules:
     * `dataModule`, `uiModule`, and `domainModule`.

2.   **`dataModule`**
     *   Tells Koin how to create the implementations of `AudioRepository` (for fetching data) and `AudioPlayer` (for playing sounds).
     *   These are set up as `singletons`, available for the entire application lifecycle.

3.  **`:domainModule`**
    *   Define use cases as `factory` instances.
    *   Koin knows how to create a new instance of a use case (like `LoadAudioUseCase`) whenever one is needed.
    *   These use cases grab what they need from Koin (ex `AudioPlayer`)

4   **`:uiModule`**
    *   We tell Koin how to create our `WaveViewModel`.
    *   Koin sees that the `WaveViewModel` use cases from the `:domain` module, and it automatically provides them.  Koin sees that the `WaveViewModel` use cases from the `:domain` module, and it automatically provides them.


# Waveform Generation Strategy

1.  **Header Parsing:**
    *   The `WavHeaderParser` in (`:domain`) first reads the WAV file's header.
    *   It validates that the file is in the expected format (16-bit mono PCM)
    *   It extracts metadata like sample rate and the location/size of the audio data.
2.  **InputStream Processing and Segmentation**: 
    *   The `WavStreamProcessor` in (`:domain`) then processes the raw audio data stream.
    *   The entire audio stream is conceptually divided into a predefined number of segments (`targetSegments`). 
    *   This number determines the horizontal resolution of the displayed waveform.
    *   The processor reads the audio data sample by sample. 
    *   Each 16-bit sample is normalized to a floating-point value representing its amplitude.
    *   For each segment, the processor tracks the minimum and maximum normalized amplitudes encountered among all samples that fall within that segment.
3.  **Data for Plotting**: 
    * The output is a list of `WaveformSegment` objects, where each object contains the minimum and maximum amplitude for its corresponding time slice of the audio. 
    * The `:ui` module then uses this data to render the waveform.
    * It draws a vertical line for each segment from its minimum to its maximum amplitude value.


# Author note (`@hypnozee`):

I've had a lot of fun building this app :)
Switching from usual API consumption and UI creation I'm currently doing.
I've had the chance to revise and use the knowledge I've gained during one of the interesting subjects in CS college:
Multimedia Signal Processing.
I've also combined this with what I like to do most: mobile apps.
The tech stack used here is state of the art and I've hopefully emphasized what a modern, performant, scalable and UI friendly Android app should look like.
Looking forward to having a constructive discussion about each of my choices and why I made them.
