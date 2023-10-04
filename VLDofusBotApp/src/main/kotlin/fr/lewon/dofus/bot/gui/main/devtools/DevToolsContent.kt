package fr.lewon.dofus.bot.gui.main.devtools

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.lewon.dofus.bot.gui.custom.handPointerIcon
import fr.lewon.dofus.bot.gui.main.devtools.d2o.D2ODevToolsContent
import fr.lewon.dofus.bot.gui.main.devtools.fight.FightDevToolsContent
import fr.lewon.dofus.bot.gui.util.AppColors

private val selectedDevTool = mutableStateOf(DevTools.entries.first())

@Composable
fun DevToolsContent() {
    Column {
        DevToolsSelectorContent()
        Row(Modifier.fillMaxSize()) {
            selectedDevTool.value.content()
        }
    }
}

private enum class DevTools(val title: String, val content: @Composable () -> Unit) {
    D2O("D2O", { D2ODevToolsContent() }),
    FIGHT("Fight", { FightDevToolsContent() })
}

@Composable
private fun DevToolsSelectorContent() {
    TabRow(
        selectedTabIndex = selectedDevTool.value.ordinal,
        Modifier.height(30.dp).width((DevTools.entries.size * 150).dp),
        backgroundColor = MaterialTheme.colors.background,
        contentColor = AppColors.primaryLightColor,
    ) {
        for (devTool in DevTools.entries) {
            Tab(
                text = { Text(devTool.title) },
                modifier = Modifier.handPointerIcon(),
                selected = selectedDevTool.value == devTool,
                unselectedContentColor = Color.LightGray,
                onClick = { selectedDevTool.value = devTool },
                enabled = true
            )
        }
    }
}