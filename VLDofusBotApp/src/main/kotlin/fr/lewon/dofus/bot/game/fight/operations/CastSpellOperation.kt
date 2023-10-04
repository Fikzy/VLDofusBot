package fr.lewon.dofus.bot.game.fight.operations

import fr.lewon.dofus.bot.core.model.spell.DofusSpellLevel

class CastSpellOperation(val spell: DofusSpellLevel, val targetCellId: Int) : FightOperation()