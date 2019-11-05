package dog.del.commons

private val allowedBlocks = arrayOf(
    Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS,
    Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS_AND_ARROWS,
    Character.UnicodeBlock.MISCELLANEOUS_SYMBOLS,
    Character.UnicodeBlock.EMOTICONS,
    Character.UnicodeBlock.TRANSPORT_AND_MAP_SYMBOLS
)

private val allowedChars = charArrayOf(
    '-',
    '_'
)

fun String.validSlug() = length >= 3 && codePoints().allMatch {
    Character.isLetterOrDigit(it) || Character.UnicodeBlock.of(it) in allowedBlocks || it.toChar() in allowedChars
}