package fr.lewon.dofus.bot.game.fight.fighter

import fr.lewon.dofus.bot.core.d2o.managers.entity.MonsterManager
import fr.lewon.dofus.bot.core.fighter.IDofusFighter
import fr.lewon.dofus.bot.core.fighter.PlayerType
import fr.lewon.dofus.bot.core.model.entity.DofusMonster
import fr.lewon.dofus.bot.core.model.spell.DofusSpellLevel
import fr.lewon.dofus.bot.game.DofusCell
import fr.lewon.dofus.bot.sniffer.model.types.game.context.fight.*
import kotlin.math.min

class Fighter(
    var cell: DofusCell,
    var id: Double,
    var fighterInfo: GameFightFighterInformations = GameFightFighterInformations(),
    var spells: List<DofusSpellLevel> = ArrayList(),
    val baseStatsById: MutableMap<Int, Int> = HashMap(),
    val statsById: MutableMap<Int, Int> = HashMap(),
    val stateBuffs: MutableMap<String, StateBuff> = HashMap(),
) : IDofusFighter {

    var monsterProperties: DofusMonster? = null
    var bonesId: Int = 0
    var teamId = 0
    var invisibilityState = 0
    var telefraggedThisTurn = false
    var previousCellIds = mutableListOf<Int>()

    var maxHp = 0
    var hpLost = 0
    var hpHealed = 0
    var baseHp = 0
    var shield = 0
    var totalMp = 0

    init {
        initFighterInfo(fighterInfo)
    }

    fun initFighterInfo(fighterInfo: GameFightFighterInformations) {
        teamId = fighterInfo.spawnInfo.teamId
        invisibilityState = fighterInfo.stats.invisibilityState
        monsterProperties = if (fighterInfo is GameFightMonsterInformations) {
            MonsterManager.getMonster(fighterInfo.creatureGenericId.toDouble())
        } else null
    }

    override fun getFighterTeamId(): Int {
        return teamId
    }

    override fun getFighterId(): Double {
        return id
    }

    override fun getPlayerType(): PlayerType {
        if (this.fighterInfo is GameFightFighterNamedInformations) {
            return PlayerType.HUMAN
        }
        if (this.fighterInfo is GameFightEntityInformation) {
            return PlayerType.SIDEKICK
        }
        if (this.fighterInfo is GameFightAIInformations) {
            return PlayerType.MONSTER
        }
        return PlayerType.UNKNOWN
    }

    override fun getBreed(): Int {
        val fighterInfo = this.fighterInfo
        if (fighterInfo is GameFightCharacterInformations) {
            return fighterInfo.breed
        }
        if (fighterInfo is GameFightMonsterInformations) {
            return fighterInfo.creatureGenericId
        }
        return -1
    }

    fun isVisible(): Boolean {
        return invisibilityState != 1
    }

    override fun isSummon(): Boolean {
        return fighterInfo.stats.summoned
    }

    override fun isStaticElement(): Boolean {
        return false
    }

    override fun hasState(state: Int): Boolean {
        return stateBuffs.values.any { it.stateId == state }
    }

    fun useSummonSlot(): Boolean {
        return monsterProperties?.useSummonSlot ?: false
    }

    override fun getSummonerId(): Double {
        return fighterInfo.stats.summoner
    }

    override fun wasTelefraggedThisTurn(): Boolean {
        return telefraggedThisTurn
    }

    fun canBreedSwitchPos(): Boolean {
        return monsterProperties?.canSwitchPos != true
    }

    fun canBreedSwitchPosOnTarget(): Boolean {
        return monsterProperties?.canSwitchPosOnTarget != true
    }

    fun canBreedBePushed(): Boolean {
        return monsterProperties?.canBePushed != true
    }

    fun deepCopy(): Fighter {
        return Fighter(
            cell, id, fighterInfo, spells, baseStatsById.toMutableMap(),
            statsById.toMutableMap(), stateBuffs.mapValues { it.value.copy() }.toMutableMap()
        ).also {
            it.maxHp = maxHp
            it.hpLost = hpLost
            it.hpHealed = hpHealed
            it.baseHp = baseHp
            it.teamId = teamId
            it.totalMp = totalMp
            it.invisibilityState = invisibilityState
            it.previousCellIds = previousCellIds.toMutableList()
            it.telefraggedThisTurn = telefraggedThisTurn
            it.bonesId = bonesId
            it.monsterProperties = monsterProperties
        }
    }

    fun getCurrentHp(): Int {
        return min(maxHp + shield, baseHp + shield - hpLost + hpHealed)
    }

    fun addStateBuff(uid: String, turnDuration: Int, stateId: Int) {
        this.stateBuffs[uid] = StateBuff(stateId, turnDuration)
    }
}