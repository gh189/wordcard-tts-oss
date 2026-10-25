# Wordcard TTS

An offline Android text-to-speech (TTS) engine that gives the Wordcard app AI English pronunciation.

- Voices: [Piper](https://huggingface.co/rhasspy/piper-voices) VITS medium voices (int8, MIT), run on the device by [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx). No network needed.
- Accents: British cori (`en-GB`) and American LJSpeech (`en-US` and other English regions), both trained on public-domain recordings.
- Implements the system `TextToSpeechService`, so other apps can use it once it is selected under the system "Text-to-speech output" settings.

## Using it from an app

Pass this engine's package name to the standard `TextToSpeech` API:

```kotlin
val tts = TextToSpeech(context, { status -> /* … */ }, "com.gh189.wordcard.tts")
tts.language = Locale.UK  // or Locale.US
tts.speak("hello", TextToSpeech.QUEUE_FLUSH, null, "id")
```

On Android 11 and later the calling app must declare TTS visibility in its `AndroidManifest.xml`:

```xml
<queries>
    <intent>
        <action android:name="android.intent.action.TTS_SERVICE" />
    </intent>
</queries>
```

## Building

Requires the Android SDK (set `ANDROID_HOME` or `sdk.dir` in `local.properties`). Gradle runs on JDK 25, as set in `gradle/gradle-daemon-jvm.properties`, and downloads it if it is missing.

```bash
./gradlew testDebugUnitTest assembleDebug
```

The APK is written to `app/build/outputs/apk/`. Both voice models (about 37 MB) are bundled in the app.

Release builds are signed only when a `keystore.properties` file (not included) provides `storeFile`, `storePassword`, `keyAlias` and `keyPassword`; without it `assembleRelease` produces an unsigned APK.

## License

Copyright © 2026 gh189

Wordcard TTS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the [GNU General Public License](LICENSE) for more details.

You should have received a copy of the GNU General Public License along with this program. If not, see <https://www.gnu.org/licenses/>.

## Third-party components

This repository includes these unmodified third-party files, each under its own license:

| Path | Component | License | Source |
|---|---|---|---|
| `app/libs/sherpa-onnx-1.13.8.aar` | sherpa-onnx 1.13.8 (with ONNX Runtime, eSpeak NG, piper-phonemize and other native libraries) | Apache-2.0 | <https://github.com/k2-fsa/sherpa-onnx/tree/v1.13.8> |
| `app/src/main/assets/tts/espeak-ng-data/` | eSpeak NG data | GPL-3.0-or-later | <https://github.com/csukuangfj/espeak-ng/tree/ed530aa113046142eb5115cf2fc9157854d0ffe1> |
| `app/src/main/assets/tts/piper/` | Piper voices en_GB-cori-medium, en_US-ljspeech-medium (int8 builds from sherpa-onnx) | MIT | <https://huggingface.co/rhasspy/piper-voices> |

eSpeak NG is linked through piper-phonemize (<https://github.com/csukuangfj/piper-phonemize/tree/f3ff95afc03640bc1399e113e83361192a2fafb4>). The full list of components and their license texts is shown on the app's home screen and kept in `app/src/main/assets/licenses/`.
