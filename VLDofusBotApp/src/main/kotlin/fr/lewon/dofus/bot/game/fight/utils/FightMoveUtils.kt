package fr.lewon.dofus.bot.game.fight.utils

import fr.lewon.dofus.bot.game.DofusCell
import fr.lewon.dofus.bot.game.fight.FightBoard

object FightMoveUtils {

    fun getMoveCells(fightBoard: FightBoard, range: Int, fromCell: DofusCell): List<DofusCell> {
        val enemiesCellIds = fightBoard.getEnemyFighters().map { it.cell.cellId }
        val accessibleCells = mutableListOf(fromCell)
        val explored = mutableListOf(fromCell.cellId)
        var frontier = mutableListOf(fromCell)
        for (i in 0 until range) {
            val newFrontier = ArrayList<DofusCell>()
            for (cell in frontier) {
                val accessibleNeighbors = getCellAccessibleNeighbors(fightBoard, cell, explored)
                for (neighbor in accessibleNeighbors) {
                    explored.add(neighbor.cellId)
                    val isNeighborNextToEnemy = neighbor.neighbors.any { it.cellId in enemiesCellIds }
                    if (!isNeighborNextToEnemy) {
                        newFrontier.add(neighbor)
                    }
                    accessibleCells.add(neighbor)
                }
            }
            frontier = newFrontier
        }
        return accessibleCells
    }

    private fun getCellAccessibleNeighbors(
        fightBoard: FightBoard,
        cell: DofusCell,
        exploredCellIds: List<Int>
    ): List<DofusCell> = cell.neighbors.filter { neighbor ->
        !exploredCellIds.contains(neighbor.cellId) && neighbor.isAccessible() && !fightBoard.isFighterHere(neighbor)
    }

}