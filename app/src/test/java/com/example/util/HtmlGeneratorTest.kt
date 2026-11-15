package com.example.util

import com.example.data.model.ThemeFinish
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlGeneratorTest {

    @Test
    fun buildWebsiteHtml_escapesUnsafeTagsAndIncludesThemePalette() {
        val html = HtmlGenerator.buildWebsiteHtml(
            title = "Ekativa <script>Gold</script>",
            heading = "Pure Heritage",
            content = "Finest jewelry & custom chains.",
            finish = ThemeFinish.PLATINUM
        )

        assertTrue(html.contains("Ekativa &lt;script&gt;Gold&lt;/script&gt;"))
        assertTrue(html.contains("Finest jewelry &amp; custom chains."))
        assertTrue(html.contains(ThemeFinish.PLATINUM.primaryHex))
        assertTrue(html.contains("PLATINUM EDITION"))
        assertFalse(html.contains("<script>Gold</script>"))
    }
}
