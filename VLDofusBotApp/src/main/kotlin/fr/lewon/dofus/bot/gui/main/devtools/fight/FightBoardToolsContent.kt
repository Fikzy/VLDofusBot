package fr.lewon.dofus.bot.gui.main.devtools.fight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import fr.lewon.dofus.bot.gui.custom.CommonText
import fr.lewon.dofus.bot.gui.custom.handPointerIcon
import fr.lewon.dofus.bot.gui.util.AppColors

@Composable
fun FightBoardToolsContent() {
    Column {
        Button(
            onClick = { FightDevToolsUiUtil.skipTurn() },
            shape = RectangleShape,
            colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.backgroundColor),
            contentPadding = PaddingValues(0.dp),
            enabled = true
        ) {
            CommonText(
                text = "Skip turn",
                modifier = Modifier.fillMaxWidth().handPointerIcon().padding(5.dp)
                    .align(Alignment.CenterVertically)
            )
        }
        val buttonSize = 35.dp
        for (fightBoardTool in FightBoardTools.entries) {
            val selected = FightDevToolsUiUtil.selectedFightBoardTool.value == fightBoardTool
            Row(Modifier.padding(3.dp)) {
                val modifier = if (selected) {
                    Modifier.border(BorderStroke(2.dp, AppColors.primaryLightColor))
                } else Modifier
                Row(modifier.height(buttonSize)) {
                    Button(
                        onClick = { FightDevToolsUiUtil.selectedFightBoardTool.value = fightBoardTool },
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.backgroundColor),
                        contentPadding = PaddingValues(0.dp),
                        enabled = true
                    ) {
                        CommonText(
                            text = fightBoardTool.label,
                            modifier = Modifier.fillMaxWidth().handPointerIcon().padding(5.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                }
            }
        }
    }
}