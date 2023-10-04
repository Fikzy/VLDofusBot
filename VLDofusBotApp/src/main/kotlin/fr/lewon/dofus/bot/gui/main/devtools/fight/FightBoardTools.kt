package fr.lewon.dofus.bot.gui.main.devtools.fight

import fr.lewon.dofus.bot.game.DofusCell

enum class FightBoardTools(val label: String, val handleClickOnCellId: (DofusCell?) -> Unit) {
    PlaceCharacter("Place Character", {
        it?.let(FightDevToolsUiUtil::placeCharacter)
    }),
    PlaceAlly("Place Ally", {
        it?.let(FightDevToolsUiUtil::placeAlly)
    }),
    PlaceEnemy("Place Enemy", {
        it?.let(FightDevToolsUiUtil::placeEnemy)
    }),
    RemoveFighter("Remove Fighter", {
        it?.let(FightDevToolsUiUtil::removeFighter)
    }),
    CastSpell("Cast Spell", {
        it?.let(FightDevToolsUiUtil::castSpell)
    })
}