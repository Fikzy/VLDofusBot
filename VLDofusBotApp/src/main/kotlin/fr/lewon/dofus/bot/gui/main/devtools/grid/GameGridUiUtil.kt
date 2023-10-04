package fr.lewon.dofus.bot.gui.main.devtools.grid

import androidx.compose.ui.geometry.Offset
import fr.lewon.dofus.bot.game.DofusBoard
import fr.lewon.dofus.bot.gui.ComposeUIUtil

object GameGridUiUtil : ComposeUIUtil() {

    const val GridWidth = 800
    const val GridHeight = 600
    const val TileWidth = GridWidth / (DofusBoard.MAP_WIDTH.toFloat() + 0.5f)
    const val TileHeight = GridHeight / (DofusBoard.MAP_HEIGHT.toFloat() + 0.5f)

    private val cellPointsByCellId = DofusBoard().cells.associate {
        val center = it.getCenter()
        val xCenter = center.x * GridWidth
        val yCenter = center.y * GridHeight / DofusBoard.HEIGHT_RATIO
        it.cellId to CellPoints(
            topPoint = Offset(xCenter, yCenter - TileHeight / 2),
            rightPoint = Offset(xCenter + TileWidth / 2, yCenter),
            bottomPoint = Offset(xCenter, yCenter + TileHeight / 2),
            leftPoint = Offset(xCenter - TileWidth / 2, yCenter),
        )
    }

    fun getCellPoints(cellId: Int) = cellPointsByCellId[cellId]
        ?: error("Cell not found : $cellId")

    fun getCellPointsById() = cellPointsByCellId.toMap()

}