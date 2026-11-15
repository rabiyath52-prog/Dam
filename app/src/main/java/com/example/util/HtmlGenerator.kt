package com.example.util

import com.example.data.model.ThemeFinish

object HtmlGenerator {

    fun buildWebsiteHtml(
        title: String,
        heading: String,
        content: String,
        finish: ThemeFinish
    ): String {
        val safeTitle = escapeHtml(title.ifBlank { "Ekativa Showcase" })
        val safeHeading = escapeHtml(heading.ifBlank { "Crafted with Precision" })
        val safeContent = escapeHtml(
            content.ifBlank { "Add your brand story, featured offerings, or upload a photo to auto-generate website copy." }
        ).replace("\n", "<br/>")
        val badgeLabel = "${finish.displayName.uppercase()} EDITION"

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="utf-8" />
              <meta name="viewport" content="width=device-width, initial-scale=1.0" />
              <title>$safeTitle</title>
              <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                body {
                  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                  background: ${finish.pageBgHex};
                  color: ${finish.textHex};
                  padding: 18px;
                  line-height: 1.6;
                }
                .topbar {
                  display: flex;
                  justify-content: space-between;
                  align-items: center;
                  padding-bottom: 14px;
                  border-bottom: 1px solid rgba(255,255,255,0.12);
                  margin-bottom: 18px;
                }
                .brand {
                  font-size: 15px;
                  font-weight: 700;
                  letter-spacing: 0.04em;
                  color: ${finish.primaryHex};
                }
                .badge {
                  font-size: 10px;
                  font-weight: 700;
                  letter-spacing: 0.1em;
                  padding: 4px 10px;
                  border-radius: 999px;
                  border: 1px solid ${finish.primaryHex};
                  color: ${finish.primaryHex};
                  background: rgba(255, 255, 255, 0.04);
                }
                .hero {
                  background: linear-gradient(145deg, ${finish.cardBgHex}, ${finish.pageBgHex});
                  border: 1px solid rgba(255,255,255,0.14);
                  border-top: 3px solid ${finish.primaryHex};
                  border-radius: 16px;
                  padding: 22px 18px;
                  box-shadow: 0 10px 28px rgba(0, 0, 0, 0.35);
                }
                h1 {
                  font-size: 24px;
                  line-height: 1.25;
                  font-weight: 800;
                  margin-bottom: 12px;
                  background: linear-gradient(90deg, ${finish.primaryHex}, ${finish.secondaryHex});
                  -webkit-background-clip: text;
                  -webkit-text-fill-color: transparent;
                }
                p {
                  font-size: 14px;
                  color: ${finish.mutedTextHex};
                  margin-bottom: 18px;
                }
                .cta {
                  display: inline-block;
                  padding: 10px 18px;
                  border-radius: 10px;
                  font-size: 13px;
                  font-weight: 700;
                  color: #0A0A0A;
                  background: linear-gradient(135deg, ${finish.primaryHex}, ${finish.secondaryHex});
                  text-decoration: none;
                }
                .footer {
                  margin-top: 16px;
                  text-align: center;
                  font-size: 11px;
                  color: rgba(255,255,255,0.45);
                }
              </style>
            </head>
            <body>
              <header class="topbar">
                <span class="brand">$safeTitle</span>
                <span class="badge">$badgeLabel</span>
              </header>
              <main class="hero">
                <h1>$safeHeading</h1>
                <p>$safeContent</p>
                <span class="cta">Explore Collection</span>
              </main>
              <footer class="footer">Built with Ekativa Studio • ${finish.displayName} Theme</footer>
            </body>
            </html>
        """.trimIndent()
    }

    private fun escapeHtml(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&#39;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
