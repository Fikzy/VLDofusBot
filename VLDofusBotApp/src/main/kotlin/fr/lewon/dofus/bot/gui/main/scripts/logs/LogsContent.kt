package fr.lewon.dofus.bot.gui.main.scripts.logs

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import fr.lewon.dofus.bot.gui.custom.ButtonWithTooltip
import fr.lewon.dofus.bot.gui.custom.CommonText
import fr.lewon.dofus.bot.gui.custom.ExpandableText
import fr.lewon.dofus.bot.gui.custom.ExpandedContent
import fr.lewon.dofus.bot.gui.util.AppColors
import kotlinx.coroutines.delay
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
fun LogsContent(loggerType: LoggerUIType, characterName: String) {
    val loggerUIState = LogsUIUtil.getLoggerUIState(characterName, loggerType)
    Row(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxHeight().background(AppColors.VERY_DARK_BG_COLOR)
                .padding(4.dp).padding(start = 2.dp)
        ) {
            LoggerButtonsContent(loggerUIState)
        }
        LogItemsContent(loggerUIState)
    }
}

@Composable
fun LoggerButtonsContent(loggerUIState: MutableState<LoggerUIState>) {
    val iconSize = 30.dp
    Column(Modifier.width(iconSize)) {
        val autoScrollEnabled = loggerUIState.value.autoScroll
        Row(Modifier.height(iconSize)) {
            ButtonWithTooltip(
                { loggerUIState.value = loggerUIState.value.copy(autoScroll = !autoScrollEnabled) },
                "Auto scroll",
                imageVector = Icons.Default.KeyboardDoubleArrowDown,
                iconColor = if (autoScrollEnabled) Color.Black else Color.LightGray,
                width = iconSize,
                shape = RectangleShape,
                defaultBackgroundColor = if (autoScrollEnabled) AppColors.primaryLightColor else AppColors.DARK_BG_COLOR,
                hoverBackgroundColor = if (autoScrollEnabled) AppColors.primaryLightColor else Color.DarkGray,
                delayMillis = 0
            )
        }
        Divider(Modifier.fillMaxWidth().height(2.dp), color = Color.Transparent)
        if (loggerUIState.value.loggerType.canBePaused) {
            val pauseEnabled = loggerUIState.value.pauseLogs
            Row(Modifier.height(iconSize)) {
                ButtonWithTooltip(
                    { loggerUIState.value = loggerUIState.value.copy(pauseLogs = !pauseEnabled) },
                    "Pause",
                    imageVector = Icons.Default.Pause,
                    iconColor = if (pauseEnabled) Color.Black else Color.LightGray,
                    width = iconSize,
                    shape = RectangleShape,
                    defaultBackgroundColor = if (pauseEnabled) AppColors.primaryLightColor else AppColors.DARK_BG_COLOR,
                    hoverBackgroundColor = if (pauseEnabled) AppColors.primaryLightColor else Color.DarkGray,
                    delayMillis = 0
                )
            }
            Divider(Modifier.fillMaxWidth().height(2.dp), color = Color.Transparent)
        }
        Row(Modifier.height(iconSize)) {
            ButtonWithTooltip(
                { copyLogger(loggerUIState.value) },
                "Put logger content in clipboard",
                imageVector = Icons.Default.ContentCopy,
                iconColor = Color.LightGray,
                imageModifier = Modifier.padding(3.dp),
                width = iconSize,
                shape = RectangleShape,
                defaultBackgroundColor = AppColors.DARK_BG_COLOR,
                hoverBackgroundColor = Color.DarkGray,
                delayMillis = 0
            )
        }
        Divider(Modifier.fillMaxWidth().height(2.dp), color = Color.Transparent)
        Row(Modifier.height(iconSize)) {
            ButtonWithTooltip(
                {
                    loggerUIState.value = loggerUIState.value.copy(
                        logItems = emptyList(),
                        expandedLogItem = null
                    )
                },
                "Clear",
                imageVector = Icons.Default.DeleteForever,
                iconColor = Color.LightGray,
                width = iconSize,
                shape = RectangleShape,
                defaultBackgroundColor = AppColors.DARK_BG_COLOR,
                hoverBackgroundColor = Color.DarkGray,
                delayMillis = 0
            )
        }
    }
}

private fun copyLogger(loggerUIState: LoggerUIState) {
    val text = loggerUIState.logItems.joinToString("\n") {
        if (it.description.isNotEmpty()) {
            "${it.text}\n > ${it.description}"
        } else it.text
    }
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val clipboardContent = StringSelection(text)
    clipboard.setContents(clipboardContent, clipboardContent)
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LogItemsContent(loggerUIState: MutableState<LoggerUIState>) {
    val loggerUIStateValue = loggerUIState.value
    val logItems = loggerUIStateValue.logItems.toList()
    val listState = loggerUIStateValue.listState
    Column {
        Box(Modifier.fillMaxSize().weight(1f).padding(5.dp)) {
            SelectionContainer(Modifier.fillMaxSize()) {
                LazyColumn(Modifier.fillMaxSize().padding(end = 10.dp).onPointerEvent(PointerEventType.Scroll) {
                    val scrollValue = it.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                    if (scrollValue < 0 && listState.canScrollBackward) {
                        loggerUIState.value = loggerUIStateValue.copy(autoScroll = false)
                    } else if (scrollValue > 0 && !listState.canScrollForward) {
                        loggerUIState.value = loggerUIStateValue.copy(autoScroll = true)
                    }
                }, state = listState) {
                    items(items = logItems, itemContent = { logItemState ->
                        val color = logItemState.logItem.color?.let { Color(it.rgb) } ?: Color.White
                        if (logItemState.description.isEmpty()) {
                            CommonText(logItemState.text, enabledColor = color)
                        } else {
                            val expanded = loggerUIState.value.expandedLogItem == logItemState
                            ExpandableText(
                                text = logItemState.text,
                                expanded = loggerUIState.value.expandedLogItem == logItemState,
                                onExpandButtonClick = {
                                    loggerUIState.value = loggerUIState.value.copy(
                                        expandedLogItem = if (expanded) null else logItemState
                                    )
                                },
                                defaultColor = color
                            )
                        }
                    })
                }
            }
            VerticalScrollbar(
                modifier = Modifier.fillMaxHeight().width(8.dp).align(Alignment.CenterEnd),
                adapter = rememberScrollbarAdapter(listState),
            )
        }
        LaunchedEffect(loggerUIStateValue.autoScroll, loggerUIStateValue.logItems) {
            while (loggerUIStateValue.autoScroll) {
                if (loggerUIStateValue.logItems.isNotEmpty() && listState.canScrollForward) {
                    listState.scrollBy(Short.MAX_VALUE.toFloat())
                }
                delay(50)
            }
        }
        val expandedLogItem = loggerUIStateValue.expandedLogItem
        if (expandedLogItem != null && loggerUIStateValue.logItems.contains(expandedLogItem)) {
            ExpandedContent(
                title = expandedLogItem.text,
                onReduceButtonClick = { loggerUIState.value = loggerUIState.value.copy(expandedLogItem = null) },
                key = expandedLogItem,
            ) {
                CommonText(expandedLogItem.description, modifier = Modifier.padding(5.dp))
            }
        }
    }
}