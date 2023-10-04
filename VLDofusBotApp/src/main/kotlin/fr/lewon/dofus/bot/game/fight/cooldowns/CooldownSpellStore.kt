package fr.lewon.dofus.bot.game.fight.cooldowns

import fr.lewon.dofus.bot.core.model.spell.DofusSpellLevel

class CooldownSpellStore : HashMap<DofusSpellLevel, Int>() {

    fun deepCopy(): CooldownSpellStore {
        return CooldownSpellStore().also {
            it.putAll(this)
        }
    }

}