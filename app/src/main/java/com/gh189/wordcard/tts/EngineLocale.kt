package com.gh189.wordcard.tts

import android.speech.tts.TextToSpeech
import com.gh189.wordcard.tts.PiperSynthesizer.Accent

/**
 * 本引擎支持的语言：只有英语，英国用英音，其余地区用美音。
 * 框架传来的语言、地区通常是三位代码（eng、GBR），兼容两位代码。
 */
object EngineLocale {
    const val LANGUAGE = "eng"
    const val UK = "GBR"
    const val US = "USA"

    /** 系统 TTS 设置里列出的语音，格式为“语言-地区”。 */
    val voices = listOf("$LANGUAGE-$US", "$LANGUAGE-$UK")

    fun isEnglish(language: String?) = language.equals(LANGUAGE, true) || language.equals("en", true)

    fun availability(language: String?, country: String?): Int = when {
        !isEnglish(language) -> TextToSpeech.LANG_NOT_SUPPORTED
        normalizedCountry(country) != null -> TextToSpeech.LANG_COUNTRY_AVAILABLE
        else -> TextToSpeech.LANG_AVAILABLE
    }

    fun accentFor(country: String?) = if (normalizedCountry(country) == UK) Accent.UK else Accent.US

    /** 已加载语言的三位代码：没有指定或不认识的地区按美式。 */
    fun loaded(country: String?) = arrayOf(LANGUAGE, normalizedCountry(country) ?: US, "")

    private fun normalizedCountry(country: String?): String? = when (country?.uppercase()) {
        UK, "GB" -> UK
        US, "US" -> US
        else -> null
    }
}
