package fr.lewon.dofus.bot.gui.main.metamob.monsters

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.lewon.dofus.bot.gui.custom.CommonText
import fr.lewon.dofus.bot.gui.custom.defaultHoverManager
import fr.lewon.dofus.bot.gui.custom.grayBoxStyle
import fr.lewon.dofus.bot.gui.main.DragTarget
import fr.lewon.dofus.bot.gui.main.metamob.MetamobHelperUIUtil
import fr.lewon.dofus.bot.gui.util.AppColors
import fr.lewon.dofus.bot.util.external.metamob.model.MetamobMonster
import fr.lewon.dofus.bot.util.external.metamob.model.MetamobMonsterType
import fr.lewon.dofus.bot.util.filemanagers.impl.MetamobConfigManager
import kotlinx.coroutines.delay

@Composable
fun MonsterCardContent(monster: MetamobMonster, key: Any) = DragTarget(
    monster, Modifier.defaultHoverManager(
        onHover = {
            MetamobHelperUIUtil.uiState.value = MetamobHelperUIUtil.uiState.value.copy(hoveredMonster = monster)
        },
        onExit = {
            MetamobHelperUIUtil.uiState.value = MetamobHelperUIUtil.uiState.value.copy(hoveredMonster = null)
        },
        key = key
    )
) {
    val monsterPainter = rememberSaveable { mutableStateOf(MetamobHelperUIUtil.getMonsterPainter(monster)) }
    LaunchedEffect(Unit) {
        if (!MetamobHelperUIUtil.isLoaded(monster.imageUrl)) {
            MetamobHelperUIUtil.loadImagePainter(monster)
        }
        while (monsterPainter.value == null) {
            delay(100)
            monsterPainter.value = MetamobHelperUIUtil.getMonsterPainter(monster)
        }
    }
    val content = remember {
        movableContentOf {
            Column(Modifier.grayBoxStyle().background(Color.DarkGray)) {
                Box(Modifier.fillMaxSize().weight(1f)) {
                    monsterPainter.value?.painter?.let {
                        Image(
                            it, "",
                            Modifier.fillMaxHeight().align(Alignment.CenterEnd).padding(5.dp).padding(end = 10.dp)
                        )
                    }
                    Row(Modifier.fillMaxSize()) {
                        Column(Modifier.fillMaxWidth().padding(top = 2.dp)) {
                            val status = when {
                                monster.searched > 0 -> "Searched"
                                monster.offered > 0 -> "Offered"
                                else -> "/"
                            }
                            SelectionContainer {
                                CommonText(
                                    monster.name,
                                    Modifier.padding(4.dp),
                                    enabledColor = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                            CommonText(
                                "Owned : ${monster.amount}", Modifier.padding(4.dp), enabledColor = Color.White,
                                fontSize = 12.sp
                            )
                            CommonText(
                                "Status : $status", Modifier.padding(4.dp), enabledColor = Color.White,
                                fontSize = 12.sp
                            )
                            if (monster.type == MetamobMonsterType.ARCHMONSTER) {
                                val monsterPrice = MetamobHelperUIUtil.getPrice(monster)
                                    ?.let { "${"%,d".format(it)} K" }
                                    ?: "/"
                                CommonText(
                                    "Price : $monsterPrice", Modifier.padding(4.dp), enabledColor = Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                val simultaneousOchers = MetamobConfigManager.readConfig().getSafeSimultaneousOchers()
                val color = when {
                    monster.amount >= simultaneousOchers -> AppColors.GREEN
                    monster.amount > 0 -> AppColors.ORANGE
                    else -> AppColors.RED
                }
                Row(Modifier.fillMaxWidth().height(6.dp).background(color)) { }
            }
        }
    }
    content()
}