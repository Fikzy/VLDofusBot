package fr.lewon.dofus.bot.gui.main.devtools.fight

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPainter
import androidx.compose.ui.unit.dp
import fr.lewon.dofus.bot.core.d2o.managers.characteristic.BreedManager
import fr.lewon.dofus.bot.gui.custom.ComboBox
import fr.lewon.dofus.bot.gui.main.TooltipTarget
import fr.lewon.dofus.bot.gui.main.characters.edit.sets.bar.spells.SpellImageContent
import fr.lewon.dofus.bot.gui.main.characters.edit.sets.bar.spells.SpellTooltipContent
import fr.lewon.dofus.bot.gui.util.AppColors
import fr.lewon.dofus.bot.util.filemanagers.impl.BreedAssetManager

@Composable
fun FightSpellSelectorContent() {
    val breed = FightDevToolsUiUtil.currentBreed.value
    Column(Modifier.padding(horizontal = 5.dp)) {
        Row(Modifier.fillMaxWidth()) {
            ComboBox(
                selectedItem = breed,
                items = BreedManager.getAllBreeds(),
                onItemSelect = { FightDevToolsUiUtil.updateBreed(it) },
                getItemIconPainter = { BreedAssetManager.getAssets(it.id).simpleIcon.toPainter() },
                getItemText = { it.name }
            )
        }
        val selectedSpell = FightDevToolsUiUtil.currentSpell.value
        LazyVerticalGrid(GridCells.Fixed(4)) {
            items(FightDevToolsUiUtil.currentSpells.value) {
                val selected = selectedSpell == it
                Button(
                    onClick = { FightDevToolsUiUtil.updateSpell(it) },
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(backgroundColor = AppColors.backgroundColor)
                ) {
                    val modifier = if (selected) {
                        Modifier.border(BorderStroke(2.dp, AppColors.primaryLightColor))
                    } else Modifier
                    Row(modifier) {
                        TooltipTarget(key = it.id, tooltipContent = { SpellTooltipContent(it) }) {
                            SpellImageContent(it)
                        }
                    }
                }
            }
        }
    }
}