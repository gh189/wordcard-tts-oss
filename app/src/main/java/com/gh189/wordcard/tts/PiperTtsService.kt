package com.gh189.wordcard.tts

import android.media.AudioFormat
import android.os.SystemClock
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.util.Log
import kotlin.concurrent.thread

/**
 * 系统 TTS 引擎：Wordcard 等应用通过 android.speech.tts.TextToSpeech 绑定本服务朗读英语。
 * 框架在同一个合成线程里依次调用 onSynthesizeText，onStop 来自其他线程。
 */
class PiperTtsService : TextToSpeechService() {
    private lateinit var synthesizer: PiperSynthesizer
    @Volatile private var stopped = false
    @Volatile private var language = EngineLocale.loaded(null)

    override fun onCreate() {
        // super.onCreate 会加载默认语言，先准备好合成器
        synthesizer = PiperSynthesizer.shared(this)
        super.onCreate()
        // 绑定后就在后台加载两种口音的模型；已经加载过时立即返回
        thread(name = "piper-preload") { PiperSynthesizer.Accent.values().forEach { synthesizer.load(it) } }
    }

    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?) =
        EngineLocale.availability(lang, country)

    override fun onGetLanguage(): Array<String> = language

    /**
     * 模型还没加载进内存时报告 [FEATURE_MODEL_LOADING]，Wordcard 据此提示用户第一次发音要等一会儿。
     * 框架在 Binder 线程直接调用，不排在合成后面，加载期间也能立即返回。
     */
    override fun onGetFeaturesForLanguage(lang: String?, country: String?, variant: String?): Set<String> =
        if (EngineLocale.isEnglish(lang) && !synthesizer.isLoaded(EngineLocale.accentFor(country))) {
            setOf(FEATURE_MODEL_LOADING)
        } else {
            emptySet()
        }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        val result = EngineLocale.availability(lang, country)
        if (result != TextToSpeech.LANG_NOT_SUPPORTED) language = EngineLocale.loaded(country)
        return result
    }

    override fun onStop() {
        stopped = true
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        stopped = false
        val text = request.charSequenceText?.toString().orEmpty()
        Log.i(TAG, "Request \"$text\" ${request.language}-${request.country} rate ${request.speechRate}")
        if (!EngineLocale.isEnglish(request.language)) {
            Log.w(TAG, "Unsupported language ${request.language}")
            callback.error(TextToSpeech.ERROR_INVALID_REQUEST)
            return
        }
        val accent = EngineLocale.accentFor(request.country)
        val start = SystemClock.elapsedRealtime()
        val sampleRate = synthesizer.load(accent) ?: run {
            callback.error(TextToSpeech.ERROR_SERVICE)
            return
        }
        Log.i(TAG, "Waited ${SystemClock.elapsedRealtime() - start} ms for the model")
        callback.start(sampleRate, AudioFormat.ENCODING_PCM_16BIT, 1)

        if (text.isNotBlank()) {
            // speechRate 以 100 为正常语速
            val speed = (request.speechRate / 100f).coerceIn(MIN_SPEED, MAX_SPEED)
            val maxBytes = callback.maxBufferSize
            // 服务和首页在同一进程，合成出错时报告给调用方，不让异常带崩整个应用
            try {
                synthesize(text, accent, speed, callback, maxBytes)
            } catch (error: Throwable) {
                Log.e(TAG, "Synthesis failed", error)
                callback.error(TextToSpeech.ERROR_SYNTHESIS)
                return
            }
        }
        if (stopped) Log.i(TAG, "Stopped before finishing \"$text\"") else callback.done()
    }

    private fun synthesize(text: String, accent: PiperSynthesizer.Accent, speed: Float, callback: SynthesisCallback, maxBytes: Int) {
        synthesizer.synthesize(text, accent, speed) { samples ->
            val pcm = Pcm.normalizedPcm16(samples)
            var offset = 0
            while (offset < pcm.size && !stopped) {
                val length = minOf(maxBytes, pcm.size - offset)
                val result = callback.audioAvailable(pcm, offset, length)
                if (result != TextToSpeech.SUCCESS) {
                    Log.w(TAG, "Playback rejected audio: $result")
                    return@synthesize false
                }
                offset += length
            }
            Log.i(TAG, "Sent ${samples.size} samples")
            !stopped
        }
    }

    companion object {
        /** 与 Wordcard 约定的特性名，Wordcard 用 TextToSpeech.getFeatures 查询。 */
        const val FEATURE_MODEL_LOADING = "com.gh189.wordcard.tts.model_loading"
        private const val TAG = "PiperTtsService"
        private const val MIN_SPEED = 0.5f
        private const val MAX_SPEED = 3f
    }
}
