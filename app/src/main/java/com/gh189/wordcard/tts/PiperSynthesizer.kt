package com.gh189.wordcard.tts

import android.content.Context
import android.os.SystemClock
import android.util.Log
import android.util.LruCache
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 用 sherpa-onnx + Piper（VITS）离线合成英语语音，英音、美音各用一个小模型。
 * 整个进程共用一个实例（见 [shared]），模型用到时加载，之后一直留在内存里；
 * 每种口音单独加锁：加载、合成某种口音时只等这种口音，不受另一种口音加载的影响；可以在不同线程调用。
 */
class PiperSynthesizer private constructor(context: Context) {
    /** voice 是 assets/tts/piper 下的模型目录，两个都是公有领域数据训练的单人女声。 */
    enum class Accent(val voice: String) {
        UK("en_GB-cori-medium"),
        US("en_US-ljspeech-medium")
    }

    private val appContext = context.applicationContext
    // 并发 Map：isLoaded 不加锁，模型加载期间也能立即回答
    private val engines = ConcurrentHashMap<Accent, OfflineTts>()
    private val locks = Accent.values().associateWith { Any() }
    // 最近合成的音频：背单词时常反复点同一个词，再读时直接播放，不用再等推理
    private val recent = object : LruCache<String, List<FloatArray>>(CACHE_SAMPLES) {
        override fun sizeOf(key: String, value: List<FloatArray>) = value.sumOf { it.size }
    }

    /** 提前加载 [accent] 的模型，返回采样率；加载失败返回 null。 */
    fun load(accent: Accent): Int? = synchronized(locks.getValue(accent)) { engine(accent)?.sampleRate() }

    /** [accent] 的模型是否已加载并预热好；不等待正在进行的加载或合成。 */
    fun isLoaded(accent: Accent) = engines.containsKey(accent)

    /**
     * 合成 [text]，每生成一段（通常是一句）就交给 [onChunk]；[onChunk] 返回 false 时停止。
     * 最近完整合成过的内容直接从缓存读出。模型加载失败时返回 false。
     */
    fun synthesize(text: String, accent: Accent, speed: Float, onChunk: (FloatArray) -> Boolean): Boolean {
        val key = "$accent|$speed|$text"
        recent.get(key)?.let { chunks ->
            Log.i(TAG, "Replayed ${text.length} chars from cache")
            chunks.all(onChunk)
            return true
        }
        synchronized(locks.getValue(accent)) {
            val tts = engine(accent) ?: return false
            val start = SystemClock.elapsedRealtime()
            val chunks = mutableListOf<FloatArray>()
            var complete = true
            tts.generateWithCallback(text, 0, speed, ChunkCallback { samples ->
                chunks += samples
                onChunk(samples).also { if (!it) complete = false }
            })
            // 中途停止时只合成了前面几段，不缓存
            if (complete) recent.put(key, chunks)
            Log.i(TAG, "Synthesized ${text.length} chars in ${SystemClock.elapsedRealtime() - start} ms")
            return true
        }
    }

    /**
     * sherpa-onnx 的 JNI 按签名 invoke([F)Ljava/lang/Integer; 查找回调方法。
     * Kotlin lambda 编译成 invokedynamic，只有擦除后的 invoke(Object)，JNI 找不到会抛 NoSuchMethodError
     * 并让整个进程崩溃，所以用具名类实现，编译出带具体类型的 invoke。
     */
    private class ChunkCallback(private val onChunk: (FloatArray) -> Boolean) : (FloatArray) -> Int {
        override fun invoke(samples: FloatArray): Int = if (onChunk(samples)) 1 else 0
    }

    // 调用方持有 accent 对应的锁
    private fun engine(accent: Accent): OfflineTts? {
        engines[accent]?.let { return it }
        val dir = "$MODEL_DIR/${accent.voice}"
        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = OfflineTtsVitsModelConfig(
                    model = "$dir/model.onnx",
                    tokens = "$dir/tokens.txt",
                    dataDir = espeakDataDir().absolutePath
                ),
                numThreads = 2
            )
        )
        val start = SystemClock.elapsedRealtime()
        val tts = runCatching { OfflineTts(appContext.assets, config) }
            .onFailure { Log.e(TAG, "Failed to load ${accent.voice}", it) }
            .getOrNull() ?: return null
        val loaded = SystemClock.elapsedRealtime()
        // ONNX Runtime 第一次推理明显更慢，先合成一个短词预热，不让第一次朗读等这段时间
        runCatching { tts.generate(WARM_UP_TEXT, 0, 1f) }
            .onFailure { Log.w(TAG, "Warm-up failed", it) }
        Log.i(TAG, "${accent.voice} loaded in ${loaded - start} ms, warmed up in ${SystemClock.elapsedRealtime() - loaded} ms")
        engines[accent] = tts
        return tts
    }

    // espeak-ng 只能从文件系统读取数据，首次使用（或升级后）从 assets 复制一份
    // 两种口音可能同时加载，复制数据要互斥
    @Synchronized
    private fun espeakDataDir(): File {
        val target = File(appContext.filesDir, "espeak-ng-data")
        val marker = File(target, ".version")
        val version = BuildConfig.VERSION_CODE.toString()
        if (marker.exists() && marker.readText() == version) return target
        target.deleteRecursively()
        copyAssets("tts/espeak-ng-data", target)
        marker.writeText(version)
        return target
    }

    private fun copyAssets(path: String, target: File) {
        val children = appContext.assets.list(path).orEmpty()
        if (children.isEmpty()) {
            target.parentFile?.mkdirs()
            appContext.assets.open(path).use { input -> target.outputStream().use { input.copyTo(it) } }
            return
        }
        target.mkdirs()
        children.forEach { copyAssets("$path/$it", File(target, it)) }
    }

    companion object {
        private const val TAG = "PiperSynthesizer"
        private const val MODEL_DIR = "tts/piper"
        private const val WARM_UP_TEXT = "hello"
        // 约 1 分钟的音频（24 kHz，每个采样 4 字节，约 6 MB），够缓存几十个单词
        private const val CACHE_SAMPLES = 24_000 * 60

        @Volatile private var instance: PiperSynthesizer? = null

        /** 进程内共用的实例：调用方断开后服务会销毁重建，模型留在这里就不用重新加载。 */
        fun shared(context: Context): PiperSynthesizer =
            instance ?: synchronized(this) { instance ?: PiperSynthesizer(context).also { instance = it } }
    }
}
