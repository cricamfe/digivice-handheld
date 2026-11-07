package dev.cricamfe.digivice.core

enum class GameSource(val id: String) {
    RETROARCH("retroarch");

    companion object {
        fun fromId(id: String?): GameSource? = entries.firstOrNull { it.id == id }
    }
}
