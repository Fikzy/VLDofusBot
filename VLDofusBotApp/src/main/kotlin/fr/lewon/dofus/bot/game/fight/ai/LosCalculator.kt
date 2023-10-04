package fr.lewon.dofus.bot.game.fight.ai

import fr.lewon.dofus.bot.game.fight.FightBoard

class LosCalculator(fightBoard: FightBoard, fromCellId: Int, toCellId: Int) {

    val los: Boolean by lazy {
        fightBoard.lineOfSight(fromCellId, toCellId)
    }
}