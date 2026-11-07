package dev.cricamfe.digivice.core.domain.models

/** Port of `Backend/Domain/Models/State.cs`: the whole game state read from RAM in one pass. */
data class State(
    var player: Player = Player(),
    var importantItems: ImportantItems = ImportantItems(),
    var party: Party = Party(),
    var digimonBattle: DigimonBattle = DigimonBattle(),
    var cardBattle: CardBattle = CardBattle(),
    var auctions: Auctions = Auctions(),
    var npcs: Npcs = Npcs(),
    var journal: Journal = Journal(),
)
