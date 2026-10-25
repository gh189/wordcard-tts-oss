package com.gh189.wordcard.tts

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

/** 首页：试听本引擎、打开系统 TTS 设置，以及 GPL 声明和第三方组件许可证。 */
class MainActivity : Activity() {
    private var tts: TextToSpeech? = null
    private lateinit var speakButtons: List<Button>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.aboutMeta).text =
            getString(R.string.about_meta, BuildConfig.VERSION_NAME, getString(R.string.developer_name))

        val text = findViewById<EditText>(R.id.tryText)
        speakButtons = listOf(
            findViewById<Button>(R.id.speakUk).apply { setOnClickListener { speak(text.text.toString(), Locale.UK) } },
            findViewById<Button>(R.id.speakUs).apply { setOnClickListener { speak(text.text.toString(), Locale.US) } },
        )
        setReady(false)
        // 指定本应用的包名，试听的就是本引擎，而不是系统默认引擎
        tts = TextToSpeech(this, { status -> onTtsInit(status) }, packageName)

        findViewById<View>(R.id.ttsSettingsRow).setOnClickListener { openTtsSettings() }
        findViewById<View>(R.id.backgroundRow).setOnClickListener { openAppSettings() }
        findViewById<View>(R.id.sourceRow).setOnClickListener { openLink(getString(R.string.source_url)) }
        findViewById<View>(R.id.gplRow).setOnClickListener {
            startActivity(LicenseActivity.intent(this, getString(R.string.about_gpl_full_text), listOf(OpenSourceNotices.GPL)))
        }

        // 卡片圆角裁掉行的点按波纹
        listOf(R.id.tryCard, R.id.licenseCard, R.id.noticesCard).forEach { findViewById<View>(it).clipToOutline = true }

        val notices = OpenSourceNotices.all
        findViewById<TextView>(R.id.noticesTitle).text = getString(R.string.about_notices_section, notices.size)
        val card = findViewById<ViewGroup>(R.id.noticesCard)
        notices.forEachIndexed { i, notice ->
            if (i > 0) layoutInflater.inflate(R.layout.about_divider, card, true)
            card.addView(noticeRow(card, notice))
        }
    }

    override fun onDestroy() {
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    private fun onTtsInit(status: Int) {
        val ready = status == TextToSpeech.SUCCESS && tts != null
        findViewById<TextView>(R.id.tryStatus).text = if (ready) "" else getString(R.string.try_unavailable)
        setReady(ready)
    }

    private fun setReady(ready: Boolean) = speakButtons.forEach {
        it.isEnabled = ready
        it.alpha = if (ready) 1f else 0.4f
    }

    private fun speak(text: String, locale: Locale) {
        val engine = tts ?: return
        if (text.isBlank()) return
        // 服务和首页在同一进程，直接查模型是否已在内存；没加载时第一次试听要等一会儿
        val accent = if (locale == Locale.UK) PiperSynthesizer.Accent.UK else PiperSynthesizer.Accent.US
        if (!PiperSynthesizer.shared(this).isLoaded(accent)) {
            Toast.makeText(this, R.string.speech_loading, Toast.LENGTH_LONG).show()
        }
        engine.language = locale
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, UTTERANCE_ID)
    }

    private fun openTtsSettings() {
        try {
            startActivity(Intent(TTS_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.tts_settings_error, Toast.LENGTH_SHORT).show()
        }
    }

    /** 本应用的系统应用信息页，省电策略、自启动等都在这里。 */
    private fun openAppSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.background_settings_error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun noticeRow(parent: ViewGroup, notice: OpenSourceNotice): View {
        val row = layoutInflater.inflate(R.layout.item_notice, parent, false)
        row.findViewById<TextView>(R.id.noticeName).text = notice.name
        row.findViewById<TextView>(R.id.noticeCopyright).text = notice.copyright
        row.findViewById<TextView>(R.id.noticeLicense).apply {
            text = notice.license
            if (notice.license.startsWith("GPL")) {
                setBackgroundResource(R.drawable.license_tag_copyleft)
                setTextColor(getColor(R.color.accent))
            }
        }
        row.setOnClickListener {
            startActivity(LicenseActivity.intent(this, notice.name, notice.licenseFiles, notice.sourceUrl))
        }
        return row
    }

    private fun openLink(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.open_link_error, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val UTTERANCE_ID = "try"
        // 系统“文字转语音输出”设置页，没有公开常量
        const val TTS_SETTINGS = "com.android.settings.TTS_SETTINGS"
    }
}
