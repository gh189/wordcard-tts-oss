package com.gh189.wordcard.tts

/**
 * 应用打包的第三方组件及其许可证。
 * [licenseFiles] 是 assets 中的文本文件，按顺序拼接后在许可证页显示；
 * [sourceUrl] 用于 GPL 组件，注明对应源码的获取地址。
 */
data class OpenSourceNotice(
    val name: String,
    val copyright: String,
    val license: String,
    val licenseFiles: List<String>,
    val sourceUrl: String? = null,
)

object OpenSourceNotices {
    const val APACHE = "licenses/apache-2.0.txt"
    const val GPL = "licenses/gpl-3.0.txt"

    /** 离线发音相关的组件在前，其余依赖在后。 */
    val all = listOf(
        OpenSourceNotice(
            name = "Piper voices en_GB-cori-medium, en_US-ljspeech-medium (int8)",
            copyright = "rhasspy/piper-voices, trained by Bryce Beattie on public-domain recordings (LibriVox, LJ Speech); int8 quantized builds provided by sherpa-onnx",
            license = "MIT",
            // piper-voices 仓库以 MIT 发布、没有单独的许可证文件，与 piper-phonemize 同一作者，共用其 MIT 文本
            licenseFiles = listOf("licenses/piper-phonemize-license.txt"),
            sourceUrl = "https://huggingface.co/rhasspy/piper-voices",
        ),
        OpenSourceNotice(
            name = "sherpa-onnx 1.13.8",
            copyright = "Copyright (c) Xiaomi Corporation（k2-fsa）",
            license = "Apache-2.0",
            licenseFiles = listOf(APACHE),
            sourceUrl = "https://github.com/k2-fsa/sherpa-onnx/tree/v1.13.8",
        ),
        OpenSourceNotice(
            name = "eSpeak NG",
            copyright = "Copyright (C) 2005–2013 Jonathan Duddington；Copyright (C) 2013–2017 Reece H. Dunn",
            license = "GPL-3.0-or-later",
            licenseFiles = listOf(GPL),
            sourceUrl = "https://github.com/csukuangfj/espeak-ng/tree/ed530aa113046142eb5115cf2fc9157854d0ffe1",
        ),
        OpenSourceNotice(
            name = "piper-phonemize",
            copyright = "Copyright (c) 2023 Michael Hansen",
            license = "MIT",
            licenseFiles = listOf("licenses/piper-phonemize-license.txt"),
            sourceUrl = "https://github.com/csukuangfj/piper-phonemize/tree/f3ff95afc03640bc1399e113e83361192a2fafb4",
        ),
        OpenSourceNotice(
            name = "ONNX Runtime 1.28.2",
            copyright = "Copyright (c) Microsoft Corporation",
            license = "MIT",
            licenseFiles = listOf("licenses/onnxruntime-license.txt", "licenses/onnxruntime-third-party-notices.txt"),
        ),
        OpenSourceNotice(
            name = "OpenFst",
            copyright = "Copyright 2005–2024 Google LLC",
            license = "Apache-2.0",
            licenseFiles = listOf(APACHE),
        ),
        OpenSourceNotice(
            name = "kaldi-native-fbank",
            copyright = "Copyright (c) 2022 Xiaomi Corporation",
            license = "Apache-2.0",
            licenseFiles = listOf(APACHE),
        ),
        OpenSourceNotice(
            name = "kaldi-decoder",
            copyright = "Copyright Kaldi authors (Microsoft Corporation, Saarland University, et al.) and Xiaomi Corporation",
            license = "Apache-2.0",
            licenseFiles = listOf(APACHE),
        ),
        OpenSourceNotice(
            name = "simple-sentencepiece",
            copyright = "Copyright 2024 Wei Kang",
            license = "Apache-2.0",
            licenseFiles = listOf(APACHE),
        ),
        OpenSourceNotice(
            name = "JSON for Modern C++",
            copyright = "Copyright (c) 2013–2025 Niels Lohmann",
            license = "MIT",
            licenseFiles = listOf("licenses/nlohmann-json-license.txt"),
        ),
        OpenSourceNotice(
            name = "hclust-cpp（fastcluster）",
            copyright = "© 2011 Daniel Müllner；© 2018 Christoph Dalitz",
            license = "BSD-2-Clause",
            licenseFiles = listOf("licenses/hclust-cpp-license.txt"),
        ),
        OpenSourceNotice(
            name = "Kotlin Standard Library",
            copyright = "Copyright JetBrains s.r.o. and Kotlin Programming Language contributors",
            license = "Apache-2.0",
            licenseFiles = listOf(APACHE),
        ),
    )

    /**
     * 把按固定列宽硬换行的许可证文本拆成段落，并把段内换行合并成空格，
     * 方便在手机上自动换行。缩进比其余行更深的行和列表项保留换行
     * （首行不参与比较，以兼容悬挂缩进的条款编号）。
     */
    fun paragraphs(text: String): List<String> =
        text.replace("\r\n", "\n")
            .split(Regex("\n[ \t]*\n+"))
            .mapNotNull { block ->
                val lines = block.lines().filter { it.isNotBlank() }
                if (lines.isEmpty()) return@mapNotNull null
                val base = lines.drop(1).minOfOrNull(::indentOf) ?: 0
                lines.foldIndexed(StringBuilder()) { i, out, line ->
                    if (i > 0) out.append(if (indentOf(line) > base || listMarker.containsMatchIn(line)) "\n" else " ")
                    out.append(line.trim())
                }.toString()
            }

    private val listMarker = Regex("""^\s*([-*•]|\d+[.)]|\([a-z0-9]+\))\s""")

    private fun indentOf(line: String) = line.indexOfFirst { !it.isWhitespace() }
}
