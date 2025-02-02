package fr.lewon.dofus.bot.gui.main.characters.edit.sets.bar.spells

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.lewon.dofus.bot.core.d2o.managers.characteristic.BreedManager
import fr.lewon.dofus.bot.core.d2o.managers.spell.SpellManager
import fr.lewon.dofus.bot.core.d2o.managers.spell.SpellVariantManager
import fr.lewon.dofus.bot.core.model.spell.DofusSpell
import fr.lewon.dofus.bot.gui.custom.CommonText
import fr.lewon.dofus.bot.gui.custom.HorizontalSeparator
import fr.lewon.dofus.bot.gui.main.characters.CharacterUIState
import fr.lewon.dofus.bot.gui.main.characters.edit.sets.CharacterSetsUiUtil
import fr.lewon.dofus.bot.gui.main.characters.edit.sets.bar.CharacterElementBar
import fr.lewon.dofus.bot.gui.main.characters.edit.sets.bar.CharacterElementBarEditionContent
import fr.lewon.dofus.bot.gui.main.characters.edit.sets.bar.ElementItemType
import fr.lewon.dofus.bot.gui.util.UiResource
import fr.lewon.dofus.bot.util.filemanagers.impl.SpellAssetManager

@Composable
fun CharacterSpellsEditionContent(characterUIState: CharacterUIState) {
    val classSpells = SpellVariantManager.getSortedSpells(characterUIState.dofusClassId)
    val additionalSpells = SpellVariantManager.getSortedSpells(BreedManager.anyBreedId)
    CharacterElementBarEditionContent(
        characterUIState = characterUIState,
        setElements = CharacterSetsUiUtil.getCurrentSet(characterUIState.name).spells,
        itemType = ElementItemType.SPELL,
        availableElements = classSpells.plus(additionalSpells),
        getElementById = { SpellManager.getSpell(it) },
        getElementName = { it.name },
        getElementImageContent = { SpellImageContent(it) },
        getElementId = { it.id },
        getElementTooltipContent = { SpellTooltipContent(it) },
        updateElementId = { key, ctrlModifier, itemId ->
            CharacterSetsUiUtil.updateSpellId(characterUIState.name, key, ctrlModifier, itemId)
        }
    )
}

@Composable
fun CharacterSpellBar(characterName: String, includeTitle: Boolean = true) = CharacterElementBar(
    itemType = ElementItemType.SPELL,
    setElements = CharacterSetsUiUtil.getCurrentSet(characterName).spells,
    getElementById = { SpellManager.getSpell(it) },
    getElementId = { it.id },
    getElementTooltipContent = { SpellTooltipContent(it) },
    getElementImageContent = { SpellImageContent(it) },
    updateElementId = { key, ctrlModifier, itemId ->
        CharacterSetsUiUtil.updateSpellId(characterName, key, ctrlModifier, itemId)
    },
    includeTitle = includeTitle
)

@Composable
fun SpellImageContent(spell: DofusSpell) {
    val isParsedCompletely = spell.levels.all { it.isParsedCompletely }
    val isEffectsEmpty = spell.levels.any { it.effects.isEmpty() }
    SpellAssetManager.getIconPainter(spell.id)?.let { painter ->
        Box(Modifier.fillMaxSize()) {
            Image(
                painter,
                "",
                Modifier.fillMaxSize().align(Alignment.Center),
                alpha = if (isEffectsEmpty) 0.3f else 1f
            )
            if (!isParsedCompletely) {
                Image(
                    UiResource.WARNING.imagePainter,
                    "",
                    Modifier.fillMaxSize().align(Alignment.Center),
                    alpha = 0.6f,
                    colorFilter = ColorFilter.tint(if (isEffectsEmpty) Color.Black else Color.Yellow)
                )
            }
        }
    }
}

@Composable
fun SpellTooltipContent(spell: DofusSpell) {
    val isParsedCompletely = spell.levels.all { spellLevel -> spellLevel.isParsedCompletely }
    val isEffectsEmpty = spell.levels.any { spellLevel -> spellLevel.effects.isEmpty() }
    Column(Modifier.widthIn(max = 300.dp).padding(vertical = 10.dp)) {
        CommonText("${spell.name} (${spell.id})", fontWeight = FontWeight.Bold)
        if (isEffectsEmpty) {
            CommonText("(No effect parsed)")
        } else if (!isParsedCompletely) {
            CommonText("(Effects partially parsed)")
        }
        val spellLevel = spell.levels.lastOrNull()
        if (spellLevel != null) {
            HorizontalSeparator("General Info", modifier = Modifier.padding(vertical = 5.dp))
            val rangeCanBeBoostedText = if (spellLevel.rangeCanBeBoosted) "(Range can be boosted)" else ""
            CommonText(
                "Range : ${spellLevel.minRange} - ${spellLevel.maxRange} $rangeCanBeBoostedText",
                fontSize = 11.sp
            )
            val effects = spellLevel.effects
            if (effects.isNotEmpty()) {
                HorizontalSeparator("Effects", modifier = Modifier.padding(vertical = 5.dp))
                effects.forEach {
                    val targets = it.targets.joinToString(" / ") { target ->
                        val typeName = target.type.name
                        val typeIdSuffix = target.id?.let { id -> "($id)" } ?: ""
                        val casterOverwriteTargetStr = if (target.casterOverwriteTarget) "*" else ""
                        "$typeName$casterOverwriteTargetStr$typeIdSuffix"
                    }
                    CommonText(
                        "Targets : $targets",
                        fontSize = 11.sp
                    )
                    CommonText(
                        "Area : ${it.area.effectAreaType.name}",
                        fontSize = 11.sp
                    )
                    CommonText(
                        "Effect : ${it.effectType.name}",
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}