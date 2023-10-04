package fr.lewon.dofus.bot.game.fight.utils

import fr.lewon.dofus.bot.core.model.spell.DofusEffectArea
import fr.lewon.dofus.bot.core.model.spell.DofusEffectAreaType
import fr.lewon.dofus.bot.game.DofusBoard
import fr.lewon.dofus.bot.game.DofusCell
import fr.lewon.dofus.bot.game.fight.FightBoard
import kotlin.math.abs
import kotlin.math.sign

object FightSpellAreaUtils {

    fun getAffectedCells(
        dofusBoard: DofusBoard,
        fightBoard: FightBoard,
        fromCellId: Int,
        targetCellId: Int,
        effectZones: List<DofusEffectArea>,
    ): List<Int> {
        return effectZones.flatMap { getAffectedCells(dofusBoard, fightBoard, fromCellId, targetCellId, it) }
    }

    fun getAffectedCells(
        dofusBoard: DofusBoard,
        fightBoard: FightBoard,
        fromCellId: Int,
        targetCellId: Int,
        effectZone: DofusEffectArea,
    ): List<Int> {
        val areaSize = effectZone.size
        val fromCell = dofusBoard.getCell(fromCellId)
        val targetCell = dofusBoard.getCell(targetCellId)
        val row = targetCell.row
        val col = targetCell.col
        val cells = ArrayList<DofusCell>()
        when (effectZone.effectAreaType) {
            DofusEffectAreaType.ALL -> cells.addAll(fightBoard.getAllFighters(true).map { it.cell })
            DofusEffectAreaType.POINT -> cells.add(targetCell)
            DofusEffectAreaType.CIRCLE -> {
                for (c in col - areaSize..col + areaSize) {
                    for (r in row - areaSize..row + areaSize) {
                        if (abs(c - col) + abs(r - row) <= areaSize) {
                            dofusBoard.getCell(c, r)?.let { cells.add(it) }
                        }
                    }
                }
            }
            DofusEffectAreaType.SQUARE -> {
                for (c in col - areaSize..col + areaSize) {
                    for (r in row - areaSize..row + areaSize) {
                        dofusBoard.getCell(c, r)?.let { cells.add(it) }
                    }
                }
            }
            DofusEffectAreaType.CROSS_WITHOUT_CENTER -> {
                for (i in 1..areaSize) {
                    dofusBoard.getCell(col, row - i)?.let { cells.add(it) }
                    dofusBoard.getCell(col, row + i)?.let { cells.add(it) }
                    dofusBoard.getCell(col - i, row)?.let { cells.add(it) }
                    dofusBoard.getCell(col + i, row)?.let { cells.add(it) }
                }
            }
            DofusEffectAreaType.CROSS -> {
                for (i in 0..areaSize) {
                    dofusBoard.getCell(col, row - i)?.let { cells.add(it) }
                    dofusBoard.getCell(col, row + i)?.let { cells.add(it) }
                    dofusBoard.getCell(col - i, row)?.let { cells.add(it) }
                    dofusBoard.getCell(col + i, row)?.let { cells.add(it) }
                }
            }
            DofusEffectAreaType.DIAGONAL_CROSS -> {
                for (i in 0..areaSize) {
                    dofusBoard.getCell(col - i, row - i)?.let { cells.add(it) }
                    dofusBoard.getCell(col - i, row + i)?.let { cells.add(it) }
                    dofusBoard.getCell(col + i, row - i)?.let { cells.add(it) }
                    dofusBoard.getCell(col + i, row + i)?.let { cells.add(it) }
                }
            }
            DofusEffectAreaType.LINE -> {
                if (fromCellId == targetCellId) {
                    return emptyList()
                }
                val dCol = col - fromCell.col
                val dRow = row - fromCell.row
                val sDCol = dCol.sign
                val sDRow = dRow.sign
                val absDCol = abs(dCol)
                val absDRow = abs(dRow)
                for (i in 0..areaSize) {
                    val cell = when {
                        absDCol > absDRow -> dofusBoard.getCell(col + sDCol * i, row)
                        absDCol < absDRow -> dofusBoard.getCell(col, row + sDRow * i)
                        else -> dofusBoard.getCell(col + sDCol * i, row + sDRow * i)
                    }
                    cell?.let { cells.add(it) } ?: break
                }
            }
            DofusEffectAreaType.PERPENDICULAR_LINE -> {
                if (fromCellId == targetCellId) {
                    return emptyList()
                }
                val dCol = col - fromCell.col
                val dRow = row - fromCell.row
                val sDCol = dCol.sign
                val sDRow = dRow.sign
                val absDCol = abs(dCol)
                val absDRow = abs(dRow)
                for (i in 0..areaSize) {
                    if (absDCol > absDRow) {
                        dofusBoard.getCell(col, row + i)?.let { cells.add(it) }
                        dofusBoard.getCell(col, row - i)?.let { cells.add(it) }
                    } else if (absDCol < absDRow) {
                        dofusBoard.getCell(col + i, row)?.let { cells.add(it) }
                        dofusBoard.getCell(col - i, row)?.let { cells.add(it) }
                    } else {
                        dofusBoard.getCell(col + i, row - (sDRow * sDCol) * i)?.let { cells.add(it) }
                        dofusBoard.getCell(col - i, row + (sDRow * sDCol) * i)?.let { cells.add(it) }
                    }
                }
            }
            DofusEffectAreaType.CONE -> {
                if (fromCellId == targetCellId) {
                    return emptyList()
                }
                val dCol = col - fromCell.col
                val dRow = row - fromCell.row
                val sDCol = dCol.sign
                val sDRow = dRow.sign
                val absDCol = abs(dCol)
                val absDRow = abs(dRow)
                for (i in 0..areaSize) {
                    val coneCol = if (absDCol > absDRow) col + i * sDCol else col
                    val coneRow = if (absDCol > absDRow) row else row + i * sDRow
                    for (j in 0..i) {
                        if (absDCol > absDRow) {
                            dofusBoard.getCell(coneCol, coneRow + j)?.let { cells.add(it) }
                            dofusBoard.getCell(coneCol, coneRow - j)?.let { cells.add(it) }
                        } else if (absDCol < absDRow) {
                            dofusBoard.getCell(coneCol + j, coneRow)?.let { cells.add(it) }
                            dofusBoard.getCell(coneCol - j, coneRow)?.let { cells.add(it) }
                        }
                    }
                }
            }
        }
        return cells.filter { it.isAccessible() }
            .map { it.cellId }.distinct()
    }
}