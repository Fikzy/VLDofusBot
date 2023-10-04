package fr.lewon.dofus.bot.game.fight

import fr.lewon.dofus.bot.core.d2o.managers.entity.MonsterManager
import fr.lewon.dofus.bot.core.d2o.managers.spell.SpellManager
import fr.lewon.dofus.bot.core.model.spell.DofusSpell
import fr.lewon.dofus.bot.core.model.spell.DofusSpellLevel
import fr.lewon.dofus.bot.core.utils.LockUtils.executeSyncOperation
import fr.lewon.dofus.bot.game.DofusCell
import fr.lewon.dofus.bot.game.fight.fighter.Fighter
import fr.lewon.dofus.bot.game.fight.utils.FightSpellUtils
import fr.lewon.dofus.bot.sniffer.model.types.game.character.characteristic.CharacterCharacteristic
import fr.lewon.dofus.bot.sniffer.model.types.game.context.EntityDispositionInformations
import fr.lewon.dofus.bot.sniffer.model.types.game.context.fight.*
import fr.lewon.dofus.bot.util.filemanagers.impl.CharacterSetsManager
import fr.lewon.dofus.bot.util.network.info.GameInfo
import java.util.concurrent.locks.ReentrantLock

class FightBoard(private val gameInfo: GameInfo) {

    private val lock = ReentrantLock()
    private val dofusBoard = gameInfo.dofusBoard
    private val fightersById = HashMap<Double, Fighter>()
    val deadFighters = ArrayList<Fighter>()

    fun move(fighterId: Double, toCellId: Int, isRollback: Boolean = false) {
        lock.executeSyncOperation {
            move(getOrCreateFighterById(fighterId), toCellId, isRollback)
        }
    }

    fun move(fighter: Fighter, toCellId: Int, isRollback: Boolean = false) {
        lock.executeSyncOperation {
            move(fighter, dofusBoard.getCell(toCellId), isRollback)
        }
    }

    fun move(fighter: Fighter, toCell: DofusCell, isRollback: Boolean = false) {
        lock.executeSyncOperation {
            if (isRollback) {
                fighter.previousCellIds.removeLastOrNull()
            } else {
                fighter.previousCellIds.add(fighter.cell.cellId)
            }
            fighter.cell = toCell
        }
    }

    fun resetFighters() {
        lock.executeSyncOperation {
            fightersById.clear()
        }
    }

    fun killFighter(fighterId: Double) {
        lock.executeSyncOperation {
            fightersById.remove(fighterId)?.let { deadFighters.add(it) }
        }
    }

    fun createOrUpdateFighter(fighterInfo: GameFightFighterInformations): Fighter {
        return lock.executeSyncOperation {
            val fighterId = fighterInfo.contextualId
            val cellId = fighterInfo.disposition.cellId
            val spells = getSpellLevels(gameInfo, fighterInfo, fighterId)
            val cell = dofusBoard.getCell(cellId)
            val fighter = fightersById.computeIfAbsent(fighterId) {
                Fighter(cell, fighterId, fighterInfo)
            }
            fighter.initFighterInfo(fighterInfo)
            fighter.cell = cell
            fighter.fighterInfo = fighterInfo
            fighter.spells = spells.map(DofusSpellLevel::copy)
            fighter.teamId = fighterInfo.spawnInfo.teamId
            move(fighter, cell)
            fighter
        }
    }

    private fun getSpellLevels(
        gameInfo: GameInfo,
        fighterInfo: GameFightFighterInformations,
        fighterId: Double,
    ): List<DofusSpellLevel> {
        return when {
            fighterInfo is GameFightMonsterInformations -> {
                val spells = MonsterManager.getMonster(fighterInfo.creatureGenericId.toDouble()).spells
                getSpellLevels(spells, fighterInfo.creatureLevel)
            }
            fighterInfo is GameFightCharacterInformations && fighterId == gameInfo.playerId -> {
                val set = CharacterSetsManager.getSelectedSet(gameInfo.character.name)
                val characterSpells = set.spells
                val spellIds = characterSpells.mapNotNull { it.elementId }
                val spells = spellIds.mapNotNull { SpellManager.getSpell(it) }
                getSpellLevels(spells, fighterInfo.level)
            }
            else -> emptyList()
        }
    }

    private fun getSpellLevels(spells: List<DofusSpell>, level: Int): List<DofusSpellLevel> {
        return spells.mapNotNull { getSpellLevel(it, level) }
    }

    private fun getSpellLevel(spell: DofusSpell, level: Int): DofusSpellLevel? {
        return spell.levels.filter { it.minPlayerLevel <= level }.maxByOrNull { it.minPlayerLevel }
    }

    fun updateFighterCharacteristics(fighterId: Double, characteristics: List<CharacterCharacteristic>) {
        lock.executeSyncOperation {
            updateFighterCharacteristics(getOrCreateFighterById(fighterId), characteristics)
        }
    }

    fun updateFighterCharacteristics(fighter: Fighter, characteristics: List<CharacterCharacteristic>) {
        lock.executeSyncOperation {
            characteristics.forEach {
                fighter.statsById[it.characteristicId] = DofusCharacteristicUtil.getCharacteristicValue(it)
            }
        }
    }

    fun getPlayerFighter(): Fighter? = lock.executeSyncOperation {
        fightersById[gameInfo.playerId]
    }

    fun getEnemyFighters(): List<Fighter> = lock.executeSyncOperation {
        getFighters(true)
    }

    fun getClosestEnemy(): Fighter? = lock.executeSyncOperation {
        val playerFighter = getPlayerFighter() ?: return null
        getEnemyFighters().minByOrNull { dofusBoard.getDist(playerFighter.cell, it.cell) }
    }

    fun getAlliedFighters(): List<Fighter> = lock.executeSyncOperation {
        getFighters(false)
    }

    fun getAllFighters(withSummons: Boolean = true): List<Fighter> = lock.executeSyncOperation {
        fightersById.values.toList().filter { withSummons || !it.isSummon() }
    }

    private fun getFighters(enemy: Boolean): List<Fighter> = lock.executeSyncOperation {
        fightersById.values.filter { isFighterEnemy(it) == enemy }
    }

    fun isFighterEnemy(fighter: Fighter): Boolean {
        return getPlayerFighter()?.let {
            it.teamId != fighter.teamId
        } ?: true
    }

    fun isFighterHere(cell: DofusCell): Boolean {
        return lock.executeSyncOperation {
            getFighter(cell) != null
        }
    }

    fun getFighter(cell: DofusCell): Fighter? {
        return lock.executeSyncOperation {
            getFighter(cell.cellId)
        }
    }

    fun getFighter(cellId: Int): Fighter? {
        return lock.executeSyncOperation {
            fightersById.values.firstOrNull { it.cell.cellId == cellId }
        }
    }

    fun getFighterById(fighterId: Double): Fighter? {
        return lock.executeSyncOperation {
            fightersById[fighterId]
        }
    }

    fun getOrCreateFighterById(fighterId: Double): Fighter = lock.executeSyncOperation {
        getFighterById(fighterId) ?: createOrUpdateFighter(createDefaultFighterInformation(fighterId))
    }

    private fun createDefaultFighterInformation(fighterId: Double) =
        GameFightFighterInformations().also { fighterInfo ->
            fighterInfo.contextualId = fighterId
            fighterInfo.disposition = EntityDispositionInformations().also { it.cellId = -1 }
            fighterInfo.spawnInfo = GameContextBasicSpawnInformation().also { it.teamId = -1 }
            fighterInfo.stats = GameFightCharacteristics()
        }

    fun lineOfSight(fromCell: Int, toCell: Int): Boolean {
        return lock.executeSyncOperation {
            lineOfSight(dofusBoard.getCell(fromCell), dofusBoard.getCell(toCell))
        }
    }

    fun lineOfSight(fromCell: DofusCell, toCell: DofusCell): Boolean {
        return lock.executeSyncOperation {
            FightSpellUtils.isVisibleInLineOfSight(dofusBoard, this, fromCell, toCell)
        }
    }

    fun deepCopy(): FightBoard {
        return lock.executeSyncOperation {
            FightBoard(gameInfo).also {
                it.fightersById.putAll(fightersById.entries.map { e -> e.key to e.value.deepCopy() })
            }
        }
    }

    fun triggerNewTurn() {
        getPlayerFighter()?.let {
            it.totalMp = DofusCharacteristics.MOVEMENT_POINTS.getValue(it)
        }
        getAllFighters().forEach {
            it.stateBuffs.values.forEach { stateBuff ->
                stateBuff.turnDuration -= 1
            }
            it.stateBuffs.entries.removeIf { stateBuff ->
                stateBuff.value.turnDuration <= 0
            }
        }
    }

}