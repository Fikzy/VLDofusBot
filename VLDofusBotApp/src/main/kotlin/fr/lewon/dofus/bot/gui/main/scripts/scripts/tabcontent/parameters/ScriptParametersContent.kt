package fr.lewon.dofus.bot.gui.main.scripts.scripts.tabcontent.parameters

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.lewon.dofus.bot.gui.custom.*
import fr.lewon.dofus.bot.gui.main.scripts.scripts.ScriptTabsUIUtil
import fr.lewon.dofus.bot.model.characters.parameters.ParameterValues
import fr.lewon.dofus.bot.scripts.DofusBotScriptBuilder
import fr.lewon.dofus.bot.scripts.parameters.DofusBotParameter

@Composable
fun ScriptParametersContent() {
    val builder = ScriptTabsUIUtil.getCurrentScriptBuilder()
    val parameters = builder.getParameters()
    val parametersGroups = parameters.groupBy { it.parametersGroup }.entries
        .sortedBy { it.value.firstOrNull()?.parametersGroup ?: Int.MAX_VALUE }
        .map { it.value }
    val parameterValues = ScriptParametersUIUtil.getParameterValues(builder)
    Column(Modifier.fillMaxSize().padding(5.dp).grayBoxStyle()) {
        Row(Modifier.fillMaxWidth().height(30.dp).darkGrayBoxStyle()) {
            CommonText(
                "Parameters",
                modifier = Modifier.align(Alignment.CenterVertically).padding(start = 10.dp),
                fontWeight = FontWeight.SemiBold
            )
        }
        Column(Modifier.fillMaxSize().padding(5.dp)) {
            Box(Modifier.fillMaxSize()) {
                val state = rememberScrollState()
                Column(Modifier.verticalScroll(state).padding(end = 10.dp)) {
                    for (parameterGroup in parametersGroups) {
                        for (parameter in parameterGroup) {
                            ParameterRow(builder, parameter, parameterValues)
                        }
                        HorizontalSeparator(modifier = Modifier.padding(start = 2.dp))
                    }
                }
                VerticalScrollbar(
                    modifier = Modifier.fillMaxHeight().width(8.dp).align(Alignment.CenterEnd),
                    adapter = rememberScrollbarAdapter(state),
                )
            }
        }
    }
}

@Composable
private fun <T> ParameterRow(
    scriptBuilder: DofusBotScriptBuilder,
    parameter: DofusBotParameter<T>,
    parameterValues: ParameterValues,
) {
    if (parameter.displayCondition(parameterValues)) {
        Row(Modifier.padding(start = 3.dp, top = 5.dp, bottom = 5.dp)) {
            ParameterLine(parameter, parameterValues, onParamUpdate = {
                ScriptParametersUIUtil.updateParameterValue(scriptBuilder, parameter, it)
            })
        }
    }
}