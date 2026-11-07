package dev.cricamfe.digivice.extractor.sprites

import dev.cricamfe.digivice.extractor.disc.invalidDisc
import dev.cricamfe.digivice.extractor.image.Picture

fun partnerPortraits(sprites: FieldSprites): Map<String, Picture> {
    val characters = listOf(182, 180, 181, 34, 35, 64, 65, 66)
    val keys = characters.mapIndexed { id, character -> "actors/$character-1" to "portraits/$id" }.toMap()
    val pictures = sprites.all().mapNotNull { (key, picture) -> keys[key]?.let { it to picture } }.toMap()
    if (pictures.size != characters.size) invalidDisc("Partner portrait missing")
    return pictures
}

/** Deepen existing shadow tones without changing the pixel grid, linework or highlights. */
fun shadePortrait(picture: Picture): Picture = Picture(picture.width, picture.height, IntArray(picture.argb.size) { i ->
    val color = picture.argb[i]
    if (color ushr 24 == 0) color else {
        val r = color ushr 16 and 255
        val g = color ushr 8 and 255
        val b = color and 255
        val luminance = (54 * r + 183 * g + 19 * b) / 256.0
        val weight = ((luminance - 16) / 24).coerceIn(0.0, 1.0) * ((170 - luminance) / 100).coerceIn(0.0, 1.0)
        val factor = 1 - 0.18 * weight
        (color and 0xff000000.toInt()) or (Math.rint(r * factor).toInt() shl 16) or
            (Math.rint(g * factor).toInt() shl 8) or Math.rint(b * factor).toInt()
    }
})
