package com.freedu.personalgallary.data.model

enum class SortOrder { NEWEST, OLDEST, LARGEST, NAME }

object RuleTypes {
    const val TRASH_SCREENSHOTS = "trash_screenshots"
    const val LOCK_WHATSAPP = "lock_whatsapp"
}

object SmartKeys {
    const val RECENT = "recent"
    const val MONTH = "month"
    const val SCREENSHOTS = "shots"
    const val LONG_VIDEOS = "longvids"
    const val REELS = "reels"
}
