package fr.lewon.dofus.bot.handlers.fight

import fr.lewon.dofus.bot.core.d2o.managers.spell.SpellManager
import fr.lewon.dofus.bot.sniffer.DofusConnection
import fr.lewon.dofus.bot.sniffer.model.messages.game.actions.fight.GameActionFightSpellCastMessage
import fr.lewon.dofus.bot.sniffer.store.IEventHandler
import fr.lewon.dofus.bot.util.network.GameSnifferUtil

object GameActionFightSpellCastEventHandler : IEventHandler<GameActionFightSpellCastMessage> {

    override fun onEventReceived(socketResult: GameActionFightSpellCastMessage, connection: DofusConnection) {
        val gameInfo = GameSnifferUtil.getGameInfoByConnection(connection)
        val currentSequence = gameInfo.currentSequence
        SpellManager.getSpell(socketResult.spellId)?.let { spell ->
            spell.levels.getOrNull(socketResult.spellLevel - 1)?.let { spellLevel ->
                currentSequence.spellLevelsStarted.add(spellLevel)
            }
        }
    }
}