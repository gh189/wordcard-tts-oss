package com.gh189.wordcard.tts

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class PcmTest {
    private fun shorts(bytes: ByteArray) =
        ShortArray(bytes.size / 2) { ((bytes[it * 2].toInt() and 0xFF) or (bytes[it * 2 + 1].toInt() shl 8)).toShort() }

    @Test fun normalizesPeakAndWritesLittleEndian() {
        val pcm = shorts(Pcm.normalizedPcm16(floatArrayOf(0.1f, -0.2f, 0.05f)))
        val peak = (Pcm.PEAK * Short.MAX_VALUE).toInt()
        assertEquals(3, pcm.size)
        assertEquals(-peak.toFloat(), pcm[1].toFloat(), 1f)
        assertEquals(peak / 2f, pcm[0].toFloat(), 1f)
        assertEquals(peak / 4f, pcm[2].toFloat(), 1f)
    }

    @Test fun keepsSilenceSilent() {
        assertArrayEquals(ByteArray(4), Pcm.normalizedPcm16(floatArrayOf(0f, 0f)))
        assertArrayEquals(ByteArray(0), Pcm.normalizedPcm16(FloatArray(0)))
    }
}
