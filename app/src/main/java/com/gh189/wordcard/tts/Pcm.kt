package com.gh189.wordcard.tts

import kotlin.math.abs
import kotlin.math.roundToInt

object Pcm {
    /** 归一化后的峰值，略低于满幅以免削波。 */
    const val PEAK = 0.9f

    /**
     * 把浮点采样按峰值归一化（不同单词音量一致），再转成 16 位小端 PCM。
     * 全部静音时原样输出静音。
     */
    fun normalizedPcm16(samples: FloatArray): ByteArray {
        val peak = samples.maxOfOrNull { abs(it) } ?: 0f
        val gain = if (peak > 0f) PEAK / peak else 0f
        val bytes = ByteArray(samples.size * 2)
        samples.forEachIndexed { i, sample ->
            val value = (sample * gain * Short.MAX_VALUE).roundToInt()
            bytes[i * 2] = value.toByte()
            bytes[i * 2 + 1] = (value shr 8).toByte()
        }
        return bytes
    }
}
