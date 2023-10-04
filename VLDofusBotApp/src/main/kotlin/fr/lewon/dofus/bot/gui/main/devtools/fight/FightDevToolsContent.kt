package fr.lewon.dofus.bot.gui.main.devtools.fight

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import fr.lewon.dofus.bot.core.d2o.managers.map.MapManager
import fr.lewon.dofus.bot.game.fight.DofusCharacteristics
import fr.lewon.dofus.bot.game.fight.fighter.Fighter
import fr.lewon.dofus.bot.gui.custom.*
import fr.lewon.dofus.bot.gui.main.devtools.grid.GameGridContent
import fr.lewon.dofus.bot.gui.util.AppColors

@Composable
fun FightDevToolsContent() {
    CustomStyledColumn("Fight Dev Tool", modifier = Modifier.fillMaxSize().padding(5.dp)) {
        MapLoaderContent()
        Row {
            Column(Modifier.width(150.dp)) {
                FightBoardToolsContent()
                FightSpellSelectorContent()
            }
            val fightBoard = FightDevToolsUiUtil.fightBoard.value
            val playerFighter = FightDevToolsUiUtil.playerFighter.value
            val targetCellIds = FightDevToolsUiUtil.targetCellIds.value
            GameGridContent(
                dofusBoard = FightDevToolsUiUtil.dofusBoard,
                onCellClick = { cellId ->
                    val cell = cellId?.let { FightDevToolsUiUtil.dofusBoard.getCell(cellId) }
                    FightDevToolsUiUtil.selectedFightBoardTool.value.handleClickOnCellId(cell)
                },
                getCellColor = { cellId ->
                    val fighter = fightBoard.getFighter(cellId)
                    when {
                        fighter != null && fighter == playerFighter -> Color.Blue
                        fighter?.teamId == FightDevToolsUiUtil.AllyTeamId -> Color.Green
                        fighter?.teamId == FightDevToolsUiUtil.EnemyTeamId -> Color.Red
                        else -> null
                    }
                },
                getCellSecondaryColor = { cellId ->
                    if (cellId in targetCellIds) {
                        Color.White
                    } else null
                },
                getCellOverlayContent = { cellId ->
                    val fighter = fightBoard.getFighter(cellId)
                    if (fighter != null) {
                        { FighterOverlayContent(fighter) }
                    } else null
                }
            )
        }
    }
}

@Composable
private fun ColumnScope.FighterOverlayContent(fighter: Fighter) {
    Column(Modifier.width(150.dp)) {
        CommonText("Fighter ID : ${fighter.id}")
        HorizontalSeparator()
        CommonText("HP : ${fighter.getCurrentHp()} / ${fighter.maxHp}")
        CommonText("States : ${fighter.stateBuffs.values.map { it.stateId }.joinToString(", ")}")
        CommonText("AP : ${DofusCharacteristics.ACTION_POINTS.getValue(fighter)}")
        CommonText("MP : ${DofusCharacteristics.MOVEMENT_POINTS.getValue(fighter)}")
    }
}

@Composable
private fun MapLoaderContent() {
    val mapIdInputValue = remember { mutableStateOf(0L) }
    Column {
        Row(Modifier.height(30.dp).padding(4.dp)) {
            SubTitleText("Load map by ID", modifier = Modifier.align(Alignment.CenterVertically))
            LongTextField(
                mapIdInputValue.value.toString(),
                { mapIdInputValue.value = it.toLongOrNull() ?: 0L },
                Modifier.width(250.dp).align(Alignment.CenterVertically)
            )
            ButtonWithTooltip(
                onClick = { FightDevToolsUiUtil.updateDofusBoard(mapIdInputValue.value.toDouble()) },
                title = "Update map",
                imageVector = Icons.Default.ArrowRight,
                RectangleShape
            )
        }
        CommonText(
            "Current map : ${MapManager.getDofusMap(FightDevToolsUiUtil.currentMapId.value)}",
            modifier = Modifier.padding(4.dp)
        )
        CommonText(
            FightDevToolsUiUtil.mapIdError.value,
            modifier = Modifier.padding(4.dp),
            enabledColor = AppColors.RED
        )
    }
}