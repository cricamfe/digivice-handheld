package dev.cricamfe.digivice.core.events.models

import kotlinx.serialization.Serializable

/** Port of `Backend/Events/Models/EventType.cs`; it goes out as its name (`PlayerChanged`, ...), in this order. */
@Serializable
enum class EventType {
    InitialState,
    HealthChanged,
    PlayerChanged,
    ImportantItemsChanged,
    PartyChanged,
    DigimonBattleChanged,
    CardBattleChanged,
    AuctionsChanged,
    NpcsChanged,
    JournalChanged,

    /** Only in the app: the position of the player, sent between full cycles. See `PlayerPositionDto`. */
    PlayerPositionChanged,

    /** Only in the app: the characters of the map the field draws, sent with the full cycle when the list changes. */
    FieldActorsChanged,
    FieldChanging,
    PartnerTechniquesChanged,
    QuestOffersChanged,
}
