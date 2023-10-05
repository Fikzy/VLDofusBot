package fr.lewon.dofus.bot.game.fight.utils

import fr.lewon.dofus.bot.core.model.spell.DofusEffectAreaType
import fr.lewon.dofus.bot.core.model.spell.DofusSpellEffect
import fr.lewon.dofus.bot.core.model.spell.DofusSpellEffectType
import fr.lewon.dofus.bot.core.model.spell.DofusSpellLevel
import fr.lewon.dofus.bot.game.DofusBoard
import fr.lewon.dofus.bot.game.DofusCell
import fr.lewon.dofus.bot.game.fight.DofusCharacteristics
import fr.lewon.dofus.bot.game.fight.FightBoard
import fr.lewon.dofus.bot.game.fight.fighter.Fighter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object FightSpellUtils {

    fun getPossibleTargetCellIds(
        dofusBoard: DofusBoard,
        fightBoard: FightBoard,
        casterFighter: Fighter,
        spell: DofusSpellLevel
    ): List<Int> {
        val effectTypes = spell.effects.map { it.effectType }
        val isMovingSpell = (effectTypes.contains(DofusSpellEffectType.TELEPORT)
            || effectTypes.contains(DofusSpellEffectType.DASH)) && !spell.needTakenCell
        val isSummonSpell = effectTypes.contains(DofusSpellEffectType.SUMMON_CREATURE)
        val rawTargetCells = getRawSpellTargetCells(dofusBoard, casterFighter, spell)
        if (isMovingSpell) {
            return rawTargetCells.filter { it.isAccessible() }.map { it.cellId }
        }
        if (isSummonSpell) {
            val maxRange = min(spell.minRange + 1, getSpellMaxRange(spell, casterFighter))
            return getCellsInLine(dofusBoard, casterFighter.cell, spell.minRange, maxRange).map { it.cellId }
        }
        val rawTargetCellIds = rawTargetCells.map { it.cellId }
        val fighterCellIds = getAffectedFighters(fightBoard, casterFighter, spell.effects)
            .map { it.cell.cellId }
            .filter { it in rawTargetCellIds }
        if (spell.needTakenCell || spell.effects.all { it.area.effectAreaType == DofusEffectAreaType.POINT }) {
            return fighterCellIds
        }
        val effectAreas = spell.effects.map { it.area }
        val allCells = fighterCellIds.flatMap {
            FightSpellAreaUtils.getAffectedCells(dofusBoard, fightBoard, casterFighter.cell.cellId, it, effectAreas)
        }.filter { it in rawTargetCellIds }
        if (spell.needFreeCell) {
            return allCells.filter { !fighterCellIds.contains(it) }
        }
        return allCells
    }

    fun getRawSpellTargetCells(
        dofusBoard: DofusBoard,
        casterFighter: Fighter,
        spell: DofusSpellLevel
    ): List<DofusCell> {
        val casterCell = casterFighter.cell
        val spellMaxRange = getSpellMaxRange(spell, casterFighter)
        if (!spell.castInLine && !spell.castInDiagonal) {
            return dofusBoard.cellsAtRange(spell.minRange, spellMaxRange, casterCell)
        }
        val targetCells = ArrayList<DofusCell>()
        if (spell.castInLine) {
            targetCells.addAll(getCellsInLine(dofusBoard, casterCell, spell.minRange, spellMaxRange))
        }
        if (spell.castInDiagonal) {
            targetCells.addAll(getCellsInDiagonal(dofusBoard, casterCell, spell.minRange, spellMaxRange))
        }
        return targetCells
    }

    fun getCellsInLine(dofusBoard: DofusBoard, fromCell: DofusCell, minRange: Int, maxRange: Int): List<DofusCell> {
        val cells = ArrayList<DofusCell>()
        for (i in minRange..maxRange) {
            cells.addAll(
                listOfNotNull(
                    dofusBoard.getCell(fromCell.col + i, fromCell.row),
                    dofusBoard.getCell(fromCell.col, fromCell.row + i),
                    dofusBoard.getCell(fromCell.col - i, fromCell.row),
                    dofusBoard.getCell(fromCell.col, fromCell.row - i)
                )
            )
        }
        return cells
    }

    fun getCellsInDiagonal(dofusBoard: DofusBoard, fromCell: DofusCell, minRange: Int, maxRange: Int): List<DofusCell> {
        val cells = ArrayList<DofusCell>()
        for (i in minRange..maxRange) {
            cells.addAll(
                listOfNotNull(
                    dofusBoard.getCell(fromCell.col + i, fromCell.row + i),
                    dofusBoard.getCell(fromCell.col - i, fromCell.row + i),
                    dofusBoard.getCell(fromCell.col + i, fromCell.row - i),
                    dofusBoard.getCell(fromCell.col - i, fromCell.row - i)
                )
            )
        }
        return cells
    }

    private fun getSpellMaxRange(spell: DofusSpellLevel, currentFighter: Fighter): Int {
        return if (spell.rangeCanBeBoosted) {
            val range = DofusCharacteristics.RANGE.getValue(currentFighter)
            max(spell.minRange, spell.maxRange + range)
        } else spell.maxRange
    }

    fun getAffectedFighters(
        fightBoard: FightBoard,
        casterFighter: Fighter,
        spellEffects: List<DofusSpellEffect>
    ): List<Fighter> {
        return fightBoard.getAllFighters().filter { fighter ->
            spellEffects.any {
                it.canHitTarget(casterFighter, fighter)
            }
        }
    }

    fun isVisibleInLineOfSight(
        dofusBoard: DofusBoard,
        fightBoard: FightBoard,
        fromCell: DofusCell,
        toCell: DofusCell
    ): Boolean {
        val x0 = fromCell.col
        val y0 = fromCell.row
        val x1 = toCell.col
        val y1 = toCell.row

        var dx = abs(x1 - x0)
        var dy = abs(y1 - y0)
        var x = x0
        var y = y0
        var n = -1 + dx + dy
        val xInc = if (x1 > x0) 1 else -1
        val yInc = if (y1 > y0) 1 else -1
        var error = dx - dy
        dx *= 2
        dy *= 2

        when {
            error > 0 -> {
                x += xInc
                error -= dy
            }
            error < 0 -> {
                y += yInc
                error += dx
            }
            else -> {
                x += xInc
                error -= dy
                y += yInc
                error += dx
                n--
            }
        }

        while (n > 0) {
            val cell = dofusBoard.getCell(x, y) ?: error("Cell [$x ; $y] does not exist")
            if (cell.isWall() || cell != fromCell && cell != toCell && fightBoard.isFighterHere(cell)) {
                return false
            }
            when {
                error > 0 -> {
                    x += xInc
                    error -= dy
                }
                error < 0 -> {
                    y += yInc
                    error += dx
                }
                else -> {
                    x += xInc
                    error -= dy
                    y += yInc
                    error += dx
                    n--
                }
            }
            n--
        }
        return true
    }

}