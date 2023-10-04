package fr.lewon.dofus.bot.gui.main.devtools.grid

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

class CellPoints(
    val topPoint: Offset,
    val rightPoint: Offset,
    val bottomPoint: Offset,
    val leftPoint: Offset,
) {

    val path = Path().also { path ->
        path.moveTo(leftPoint.x, leftPoint.y)
        path.lineTo(topPoint.x, topPoint.y)
        path.lineTo(rightPoint.x, rightPoint.y)
        path.lineTo(bottomPoint.x, bottomPoint.y)
        path.lineTo(leftPoint.x, leftPoint.y)
    }

    fun getCellPointsWithOffset(offset: Offset): CellPoints = CellPoints(
        topPoint = topPoint.plus(offset),
        rightPoint = rightPoint.plus(offset),
        bottomPoint = bottomPoint.plus(offset),
        leftPoint = leftPoint.plus(offset)
    )
}