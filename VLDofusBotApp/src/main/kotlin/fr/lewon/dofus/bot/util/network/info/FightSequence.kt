package fr.lewon.dofus.bot.util.network.info

import fr.lewon.dofus.bot.core.model.spell.DofusSpellLevel

class FightSequence(val type: Int, val fighterId: Double) {

    var isFinished = false
    val subSequences = mutableListOf<FightSequence>()
    val spellLevelsStarted = mutableListOf<DofusSpellLevel>()

    fun getOngoingSequence(type: Int): FightSequence? {
        if (!isFinished && this.type == type) {
            return this
        }
        for (subSequence in subSequences) {
            subSequence.getOngoingSequence(type)?.let { return it }
        }
        return null
    }

}