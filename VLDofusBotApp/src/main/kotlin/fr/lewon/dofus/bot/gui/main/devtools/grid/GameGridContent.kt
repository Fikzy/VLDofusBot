package fr.lewon.dofus.bot.gui.main.devtools.grid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import fr.lewon.dofus.bot.game.DofusBoard
import fr.lewon.dofus.bot.game.DofusCell
import fr.lewon.dofus.bot.gui.custom.onMouseMove
import fr.lewon.dofus.bot.gui.util.AppColors

private val LineColor = Color.LightGray
private val WallColor = Color.DarkGray
private val HoleColor = Color.Black
private val AccessibleColor = Color.Gray
private val HoveredColor = AppColors.primaryColor
private val StrokeStyle = Stroke(1f)

@Composable
fun GameGridContent(
    dofusBoard: DofusBoard,
    onCellClick: (Int?) -> Unit,
    getCellColor: (Int) -> Color?,
    getCellSecondaryColor: (Int) -> Color?,
    getCellOverlayContent: (Int) -> (@Composable ColumnScope.() -> Unit)?
) {
    val hoveredCellId = remember { mutableStateOf<Int?>(null) }
    Box(Modifier.padding(10.dp)) {
        Box {
            Canvas(
                Modifier.onMouseMove { location, _ ->
                    hoveredCellId.value = getCellIdAtLocation(location)
                }.pointerInput(Unit) {
                    detectTapGestures {
                        onCellClick(getCellIdAtLocation(it))
                    }
                }.width(GameGridUiUtil.GridWidth.dp).height(GameGridUiUtil.GridHeight.dp)
            ) {
                val hoveredCellIdValue = hoveredCellId.value
                for (cell in dofusBoard.cells.sortedBy { it.cellId }) {
                    val isHovered = hoveredCellIdValue == cell.cellId
                    when {
                        cell.isAccessible() -> drawAccessibleCell(cell, isHovered, getCellColor, getCellSecondaryColor)
                        cell.isWall() -> drawWallCell(cell)
                        cell.isHole() -> drawHoleCell(cell)
                    }
                }
            }
            hoveredCellId.value?.let {
                val overlayContent = getCellOverlayContent(it)
                if (overlayContent != null) {
                    val cellPoints = GameGridUiUtil.getCellPoints(it)
                    Column(
                        Modifier.offset(cellPoints.rightPoint.x.dp, cellPoints.bottomPoint.y.dp)
                            .background(AppColors.DARK_BG_COLOR.copy(alpha = 0.7f))
                            .padding(10.dp)
                    ) {
                        overlayContent()
                    }
                }
            }
        }

    }
}

private fun DrawScope.drawAccessibleCell(
    dofusCell: DofusCell,
    isHovered: Boolean,
    getCellColor: (Int) -> Color?,
    getCellSecondaryColor: (Int) -> Color?,
) {
    val cellPoints = GameGridUiUtil.getCellPoints(dofusCell.cellId)
    val path = cellPoints.path
    val cellColor = getCellColor(dofusCell.cellId)
    val cellSecondaryColor = getCellSecondaryColor(dofusCell.cellId)
    drawPath(
        path = path,
        color = cellColor ?: cellSecondaryColor ?: AccessibleColor,
        style = Fill
    )
    if (cellColor != null && cellSecondaryColor != null) {
        drawPath(
            path = getPath(cellPoints.leftPoint, listOf(cellPoints.rightPoint, cellPoints.bottomPoint)),
            color = cellSecondaryColor,
            style = Fill
        )
    }
    if (isHovered) {
        val hoveredPath = if (cellColor != null) {
            getPath(cellPoints.rightPoint, listOf(cellPoints.topPoint, cellPoints.bottomPoint))
        } else path
        drawPath(
            path = hoveredPath,
            color = HoveredColor,
            style = Fill
        )
    }
    drawPath(
        path = path,
        color = LineColor,
        style = StrokeStyle
    )
}

private fun DrawScope.drawHoleCell(dofusCell: DofusCell) {
    val cellPoints = GameGridUiUtil.getCellPoints(dofusCell.cellId)
    val cellPath = cellPoints.path
    drawPath(
        path = cellPath,
        color = HoleColor,
        style = Fill
    )
}

private fun DrawScope.drawWallCell(dofusCell: DofusCell) {
    val cellPoints = GameGridUiUtil.getCellPoints(dofusCell.cellId)
    val upperCellPoints = cellPoints.getCellPointsWithOffset(Offset(0f, -5f))
    val upperCellPath = upperCellPoints.path
    val completePath = getPath(
        cellPoints.leftPoint, listOf(
            upperCellPoints.leftPoint,
            upperCellPoints.topPoint,
            upperCellPoints.rightPoint,
            cellPoints.rightPoint,
            cellPoints.bottomPoint,
        )
    )
    drawPath(
        path = completePath,
        color = WallColor,
        style = Fill
    )
    drawPath(
        path = completePath,
        color = LineColor,
        style = StrokeStyle
    )
    drawPath(
        path = upperCellPath,
        color = LineColor,
        style = StrokeStyle
    )
    drawLine(
        start = upperCellPoints.leftPoint,
        end = cellPoints.leftPoint,
        color = LineColor
    )
    drawLine(
        start = upperCellPoints.rightPoint,
        end = cellPoints.rightPoint,
        color = LineColor
    )
    drawLine(
        start = upperCellPoints.bottomPoint,
        end = cellPoints.bottomPoint,
        color = LineColor
    )
}

private fun getCellIdAtLocation(location: Offset): Int? {
    val tempPath = Path().also {
        it.moveTo(location.x - 1, location.y - 1)
        it.lineTo(location.x + 1, location.y - 1)
        it.lineTo(location.x + 1, location.y + 1)
        it.lineTo(location.x - 1, location.y + 1)
    }
    return GameGridUiUtil.getCellPointsById().entries.firstOrNull {
        val resultPath = Path.combine(PathOperation.Difference, tempPath, it.value.path)
        resultPath.getBounds() != tempPath.getBounds()
    }?.key
}

private fun getPath(initialPoint: Offset, points: List<Offset>): Path = Path().also { path ->
    path.moveTo(initialPoint.x, initialPoint.y)
    points.forEach {
        path.lineTo(it.x, it.y)
    }
    path.lineTo(initialPoint.x, initialPoint.y)
}