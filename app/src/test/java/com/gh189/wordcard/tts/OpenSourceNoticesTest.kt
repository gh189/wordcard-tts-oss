package com.gh189.wordcard.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OpenSourceNoticesTest {
    private val assets = File("src/main/assets")

    @Test fun everyLicenseFileIsBundled() {
        val files = OpenSourceNotices.all.flatMap { it.licenseFiles } + OpenSourceNotices.GPL
        files.forEach { assertTrue("missing asset $it", File(assets, it).isFile) }
    }

    @Test fun gplComponentsNameTheirSource() {
        OpenSourceNotices.all.filter { it.license.startsWith("GPL") }
            .forEach { assertTrue("${it.name} needs a source URL", it.sourceUrl?.startsWith("https://") == true) }
    }

    @Test fun joinsHardWrappedLinesAndKeepsParagraphs() {
        val text = "MIT License\n\nPermission is hereby granted,\nfree of charge.\n\n\nTHE SOFTWARE IS PROVIDED\n\"AS IS\"."
        assertEquals(
            listOf("MIT License", "Permission is hereby granted, free of charge.", "THE SOFTWARE IS PROVIDED \"AS IS\"."),
            OpenSourceNotices.paragraphs(text)
        )
    }

    @Test fun joinsHangingIndentAndKeepsListItems() {
        val text = "   4. Redistribution. You may\n      reproduce copies:\n\n      (a) You must give\n      a copy; and\n      (b) You must cause"
        assertEquals(
            listOf("4. Redistribution. You may reproduce copies:", "(a) You must give a copy; and\n(b) You must cause"),
            OpenSourceNotices.paragraphs(text)
        )
    }
}
