package dev.cricamfe.digivice.extractor

import dev.cricamfe.digivice.extractor.cards.CardImages
import dev.cricamfe.digivice.extractor.cards.buildCardFacts
import dev.cricamfe.digivice.extractor.disc.*
import dev.cricamfe.digivice.extractor.image.Picture
import dev.cricamfe.digivice.extractor.maps.MapRenderer
import dev.cricamfe.digivice.extractor.maps.renderWorldMaps
import dev.cricamfe.digivice.extractor.models.FighterModels
import dev.cricamfe.digivice.extractor.models.StageModels
import dev.cricamfe.digivice.extractor.sprites.*
import dev.cricamfe.digivice.extractor.text.*

enum class Piece { Text, Cards, Sprites, Icons, World, Maps, Models }

internal sealed interface ExtractionAsset {
    val key: String
    data class Image(override val key: String, val render: () -> Picture) : ExtractionAsset
    data class Text(override val key: String, val generate: () -> String) : ExtractionAsset
}

internal data class ExtractionPiece(val kind: Piece, val items: () -> List<ExtractionAsset>)
private data class ExtractionPlan(val work: List<ExtractionPiece>, val aliases: Map<String, String>)

/** Sequential, bounded-memory extraction from an already validated disc. The caller owns the disc. */
class Extractor private constructor(private val sink: AssetSink, private val plan: ExtractionPlan) {
    constructor(disc: Disc, sink: AssetSink) : this(sink, productionPlan(disc))

    internal constructor(sink: AssetSink, work: List<ExtractionPiece>, aliases: Map<String, String> = emptyMap()) :
        this(sink, ExtractionPlan(work, aliases))

    val pieces: List<Piece> = plan.work.map { it.kind }

    fun run(pause: () -> Unit = {}, progress: (Piece, Int, Int) -> Unit = { _, _, _ -> }): Manifest {
        require(sink.pictureExtension.matches(Regex("[a-z0-9]+"))) { "Invalid sink image extension" }
        val assets = linkedMapOf<String, String>()
        for (piece in plan.work) {
            // Producers and their caches are scoped to one piece, so completed pictures
            // can be collected before the large map/model renderers run.
            val items = piece.items()
            progress(piece.kind, 0, items.size)
            items.forEachIndexed { index, item ->
                pause()
                val extension = if (item is ExtractionAsset.Image) sink.pictureExtension else "json"
                val filename = "${item.key}.$extension"
                require(assets.put(item.key, filename) == null) { "Duplicate asset key: ${item.key}" }
                if (!sink.has(filename)) when (item) {
                    is ExtractionAsset.Image -> sink.picture(item.key, item.render())
                    is ExtractionAsset.Text -> sink.text(item.key, item.generate())
                }
                progress(piece.kind, index + 1, items.size)
            }
        }
        for ((alias, key) in plan.aliases) {
            val filename = requireNotNull(assets[key]) { "Missing alias target: $key" }
            require(assets.put(alias, filename) == null) { "Duplicate alias key: $alias" }
        }
        val manifest = Manifest(FORMAT_VERSION, assets.toMap())
        sink.commit(manifest)
        return manifest
    }
}

private fun productionPlan(disc: Disc): ExtractionPlan {
    val files = DiscFiles(disc)
    val table = GameFileTable(files)
    val maps = MapRenderer(files, table)
    val aliases = linkedMapOf<String, String>()
    repeat(8) { aliases["sprites/$it"] = "portraits/$it" }
    val work = listOf(
        ExtractionPiece(Piece.Text) {
            val texts by lazy { buildTexts(GameText(files)) }
            GameLanguage.entries.map { language -> ExtractionAsset.Text("texts/${language.locale}") { texts.getValue(language.locale) } } +
                ExtractionAsset.Text("tables/battle") { buildBattleCatalog(files) }
        },
        ExtractionPiece(Piece.Cards) {
            val cards = CardImages(files)
            cards.ids.map { id -> ExtractionAsset.Image("cards/$id") { cards.render(id) } } +
                ExtractionAsset.Text("tables/cards") { buildCardFacts(cards) }
        },
        ExtractionPiece(Piece.Sprites) {
            val keys = CharacterTables.data.actors.keys.sorted().flatMap { character -> (0..7).map { "actors/$character-$it" } }
            streamImages(keys) { FieldSprites(files, table).all() }
        },
        ExtractionPiece(Piece.Icons) {
            val keys = CharacterTables.data.items.map { "items/item-${it.toString(16).padStart(2, '0')}" }
            streamImages(keys) { ItemIcons(files, table).all() }
        },
        ExtractionPiece(Piece.World) {
            val worlds by lazy { renderWorldMaps(files) }
            val portraits by lazy { partnerPortraits(FieldSprites(files, table)) }
            val worldKeys = listOf("world-asuka", "world-asuka-base", "world-amaterasu", "world-amaterasu-base")
            worldKeys.map { key -> ExtractionAsset.Image(key) { worlds.getValue(key) } } +
                (0..7).map { id -> ExtractionAsset.Image("portraits/$id") { shadePortrait(portraits.getValue("portraits/$id")) } }
        },
        ExtractionPiece(Piece.Maps) {
            (maps.maps + maps.additionalMaps).map { stage -> ExtractionAsset.Image("maps/$stage") { maps.render(stage) } }
        },
        ExtractionPiece(Piece.Models) {
            val fighters = FighterModels(files, table)
            val stages = StageModels(files, table)
            fighters.enemies.map { id -> ExtractionAsset.Image("enemies/$id") { fighters.render(id, 320) } } +
                fighters.enemies.map { id -> ExtractionAsset.Image("battle-enemies/$id") { fighters.renderBattleAnimation(id) } } +
                fighters.partners.map { id -> ExtractionAsset.Image("digievolutions/$id") { fighters.renderEvolution(id, 320) } } +
                stages.stages.map { id -> ExtractionAsset.Image("battle-stages/$id") { stages.render(id) } }
        },
    )
    return ExtractionPlan(work, aliases)
}

/** Consume the existing streaming sprite APIs once, and avoid opening them at all on a complete resume. */
private fun streamImages(keys: List<String>, source: () -> Sequence<Pair<String, Picture>>): List<ExtractionAsset> {
    val iterator by lazy { source().iterator() }
    return keys.map { key -> ExtractionAsset.Image(key) {
        var found: Picture? = null
        while (iterator.hasNext()) {
            val (name, picture) = iterator.next()
            if (name == key) { found = picture; break }
        }
        found ?: invalidDisc("Sprite stream is missing $key")
    } }
}
