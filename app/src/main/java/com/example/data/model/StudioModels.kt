package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ThemeFinish(
    val displayName: String,
    val primaryHex: String,
    val secondaryHex: String,
    val pageBgHex: String,
    val cardBgHex: String,
    val textHex: String,
    val mutedTextHex: String
) {
    GOLD(
        displayName = "Gold",
        primaryHex = "#FFD700",
        secondaryHex = "#B8860B",
        pageBgHex = "#0E0C06",
        cardBgHex = "#1A160A",
        textHex = "#FFF8E1",
        mutedTextHex = "#D6C58B"
    ),
    SILVER(
        displayName = "Silver",
        primaryHex = "#D8DEE9",
        secondaryHex = "#8E99A8",
        pageBgHex = "#0D1014",
        cardBgHex = "#181D24",
        textHex = "#F5F7FA",
        mutedTextHex = "#AAB4C2"
    ),
    PLATINUM(
        displayName = "Platinum",
        primaryHex = "#E5E4E2",
        secondaryHex = "#7DD3FC",
        pageBgHex = "#0B0D12",
        cardBgHex = "#151922",
        textHex = "#FFFFFF",
        mutedTextHex = "#B6C2D1"
    );

    companion object {
        fun fromString(value: String?): ThemeFinish {
            return entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: GOLD
        }
    }
}

@IgnoreExtraProperties
data class WebsiteProject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val heading: String = "",
    val content: String = "",
    val themeFinish: String = ThemeFinish.GOLD.name,
    val generatedHtml: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PythonScript(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val code: String = "",
    val lastOutput: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class GeneratedWebsiteDraft(
    val title: String,
    val heading: String,
    val content: String,
    val themeFinish: ThemeFinish
)
