package fr.lewon.dofus.bot.core.model.spell

enum class DofusEffectAreaType(val areaTypeKey: Char) {

    POINT('P'),
    ALL('a'),
    CIRCLE('C'),
    SQUARE('G'),
    CROSS('X'),
    CROSS_WITHOUT_CENTER('Q'),
    DIAGONAL_CROSS('+'),
    LINE('L'),
    PERPENDICULAR_LINE('T'),
    CONE('V');

    companion object {

        fun fromKey(areaTypeKey: Char): DofusEffectAreaType? {
            return entries.firstOrNull { it.areaTypeKey == areaTypeKey }
        }
    }

}