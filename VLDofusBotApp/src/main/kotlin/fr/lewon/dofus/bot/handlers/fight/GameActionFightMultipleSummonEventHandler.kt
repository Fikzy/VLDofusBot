package fr.lewon.dofus.bot.handlers.fight

import fr.lewon.dofus.bot.game.fight.DofusCharacteristics
import fr.lewon.dofus.bot.game.fight.utils.FighterInfoInitializer
import fr.lewon.dofus.bot.sniffer.DofusConnection
import fr.lewon.dofus.bot.sniffer.model.messages.game.actions.fight.GameActionFightMultipleSummonMessage
import fr.lewon.dofus.bot.sniffer.store.IEventHandler
import fr.lewon.dofus.bot.util.network.GameSnifferUtil

object GameActionFightMultipleSummonEventHandler : IEventHandler<GameActionFightMultipleSummonMessage> {

    override fun onEventReceived(socketResult: GameActionFightMultipleSummonMessage, connection: DofusConnection) {
        val gameInfo = GameSnifferUtil.getGameInfoByConnection(connection)
        for (summons in socketResult.summons) {
            for (summon in summons.summons) {
                val fighterInfo = FighterInfoInitializer.buildFighterInfo(summons, summon)
                val fighter = gameInfo.fightBoard.createOrUpdateFighter(fighterInfo)
                gameInfo.fightBoard.updateFighterCharacteristics(fighter, summons.stats.characteristics.characteristics)
                val hp = DofusCharacteristics.LIFE_POINTS.getValue(fighter) +
                    DofusCharacteristics.VITALITY.getValue(fighter)
                fighter.maxHp = hp
                fighter.baseHp = hp
            }
        }
    }

}