package fr.lewon.dofus.bot.game.fight.cooldowns

class GlobalTurnUseSpellStore : HashMap<Double, TurnUseSpellStore>() {

    fun getTurnUseSpellStore(fighterId: Double): TurnUseSpellStore {
        return computeIfAbsent(fighterId) { TurnUseSpellStore() }
    }

    fun deepCopy(): GlobalTurnUseSpellStore {
        val copy = GlobalTurnUseSpellStore()
        entries.forEach {
            copy[it.key] = it.value.deepCopy()
        }
        return copy
    }

}