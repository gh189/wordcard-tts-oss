package com.gh189.wordcard.tts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import java.io.IOException

/** 显示 assets 中的许可证全文；GPL 组件在开头注明对应源码地址。 */
class LicenseActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)
        findViewById<View>(R.id.backButton).setOnClickListener { finish() }
        findViewById<TextView>(R.id.licenseTitle).text = intent.getStringExtra(EXTRA_TITLE)

        val files = intent.getStringArrayListExtra(EXTRA_FILES).orEmpty()
        val paragraphs = try {
            OpenSourceNotices.paragraphs(files.joinToString("\n\n") { file -> assets.open(file).bufferedReader().use { it.readText() } })
        } catch (_: IOException) {
            listOf(getString(R.string.license_load_error))
        }

        val list = findViewById<ListView>(R.id.licenseText)
        intent.getStringExtra(EXTRA_SOURCE)?.let { url ->
            val header = layoutInflater.inflate(R.layout.item_license_paragraph, list, false) as TextView
            header.text = getString(R.string.license_source, url)
            header.setTextColor(getColor(R.color.muted))
            list.addHeaderView(header, null, false)
        }
        list.adapter = ArrayAdapter(this, R.layout.item_license_paragraph, paragraphs)
    }

    companion object {
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_FILES = "files"
        private const val EXTRA_SOURCE = "source"

        fun intent(context: Context, title: String, files: List<String>, sourceUrl: String? = null): Intent =
            Intent(context, LicenseActivity::class.java)
                .putExtra(EXTRA_TITLE, title)
                .putStringArrayListExtra(EXTRA_FILES, ArrayList(files))
                .putExtra(EXTRA_SOURCE, sourceUrl)
    }
}
