package com.gh189.wordcard.tts

import android.speech.tts.TextToSpeech
import com.gh189.wordcard.tts.PiperSynthesizer.Accent
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class EngineLocaleTest {
    @Test fun supportsEnglishOnly() {
        assertEquals(TextToSpeech.LANG_COUNTRY_AVAILABLE, EngineLocale.availability("eng", "GBR"))
        assertEquals(TextToSpeech.LANG_COUNTRY_AVAILABLE, EngineLocale.availability("en", "us"))
        assertEquals(TextToSpeech.LANG_AVAILABLE, EngineLocale.availability("eng", "AUS"))
        assertEquals(TextToSpeech.LANG_AVAILABLE, EngineLocale.availability("eng", ""))
        assertEquals(TextToSpeech.LANG_NOT_SUPPORTED, EngineLocale.availability("zho", "CHN"))
        assertEquals(TextToSpeech.LANG_NOT_SUPPORTED, EngineLocale.availability(null, null))
    }

    @Test fun britainGetsBritishAccentAndEverywhereElseAmerican() {
        assertEquals(Accent.UK, EngineLocale.accentFor("GBR"))
        assertEquals(Accent.UK, EngineLocale.accentFor("gb"))
        assertEquals(Accent.US, EngineLocale.accentFor("USA"))
        assertEquals(Accent.US, EngineLocale.accentFor("AUS"))
        assertEquals(Accent.US, EngineLocale.accentFor(null))
    }

    @Test fun reportsLoadedLanguageInThreeLetterCodes() {
        assertArrayEquals(arrayOf("eng", "GBR", ""), EngineLocale.loaded("GB"))
        assertArrayEquals(arrayOf("eng", "USA", ""), EngineLocale.loaded("AUS"))
    }
}
