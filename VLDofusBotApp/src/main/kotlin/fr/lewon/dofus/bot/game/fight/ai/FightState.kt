package fr.lewon.dofus.bot.game.fight.ai

import fr.lewon.dofus.bot.core.model.charac.DofusCharacterBasicInfo
import fr.lewon.dofus.bot.core.model.spell.DofusSpellEffectGlobalType
import fr.lewon.dofus.bot.core.model.spell.DofusSpellLevel
import fr.lewon.dofus.bot.game.DofusBoard
import fr.lewon.dofus.bot.game.fight.DofusCharacteristics
import fr.lewon.dofus.bot.game.fight.FightBoard
import fr.lewon.dofus.bot.game.fight.ai.complements.AIComplement
import fr.lewon.dofus.bot.game.fight.cooldowns.CooldownState
import fr.lewon.dofus.bot.game.fight.fighter.Fighter
import fr.lewon.dofus.bot.game.fight.operations.CastSpellOperation
import fr.lewon.dofus.bot.game.fight.operations.FightOperation
import fr.lewon.dofus.bot.game.fight.operations.MoveOperation
import fr.lewon.dofus.bot.game.fight.operations.PassTurnOperation
import fr.lewon.dofus.bot.game.fight.utils.FightMoveUtils
import fr.lewon.dofus.bot.game.fight.utils.FightSpellUtils
import kotlin.math.abs

class FightState(
    private val fightBoard: FightBoard,
    private val cooldownState: CooldownState,
    private val aiComplement: AIComplement,
    private val dofusBoard: DofusBoard,
    private val dangerMap: DangerMap,
    private val spellSimulator: SpellSimulator = SpellSimulator(dofusBoard),
    private var lastOperation: FightOperation? = null
) {

    fun deepCopy(): FightState {
        return FightState(
            fightBoard.deepCopy(),
            cooldownState.deepCopy(),
            aiComplement,
            dofusBoard,
            dangerMap.deepCopy(),
            spellSimulator,
            lastOperation
        )
    }

    private fun getCurrentFighter(): Fighter? {
        return fightBoard.getPlayerFighter()
    }

    fun getPossibleOperations(): MutableList<FightOperation> {
        val currentFighter = getCurrentFighter() ?: return ArrayList()
        val options = ArrayList<FightOperation>()
        options.add(PassTurnOperation)
        if (canMove(currentFighter)) {
            options.addAll(getPossibleMoveOperations(currentFighter))
        }
        options.addAll(getPossibleSpellOperations(currentFighter))
        return options
    }

    private fun getPossibleMoveOperations(fighter: Fighter): List<MoveOperation> {
        val options = ArrayList<MoveOperation>()
        val currentFighterMp = DofusCharacteristics.MOVEMENT_POINTS.getValue(fighter)
        val accessibleCells = FightMoveUtils.getMoveCells(fightBoard, currentFighterMp, fighter.cell)
        for (cell in accessibleCells) {
            if (fightBoard.getFighter(cell.cellId) != null) {
                continue
            }
            val path = dofusBoard.getPath(fighter.cell, cell)
            if (path != null) {
                options.add(MoveOperation(path.map { it.cellId }))
            }
        }
        return options
    }

    private fun getPossibleSpellOperations(fighter: Fighter): List<CastSpellOperation> {
        val losCalculatorByTarget = HashMap<Int, LosCalculator>()
        val options = ArrayList<CastSpellOperation>()
        for (spell in getUsableSpells(fighter)) {
            for (cellId in FightSpellUtils.getPossibleTargetCellIds(dofusBoard, fightBoard, fighter, spell)) {
                val ac = losCalculatorByTarget.computeIfAbsent(cellId) {
                    LosCalculator(fightBoard, fighter.cell.cellId, it)
                }
                if (canCastSpellOnTarget(fighter, spell, ac, cellId, cooldownState)) {
                    options.add(CastSpellOperation(spell, cellId))
                }
            }
        }
        return options
    }

    private fun getUsableSpells(fighter: Fighter): List<DofusSpellLevel> = fighter.spells.filter {
        isSpellReady(fighter, it, cooldownState, DofusCharacteristics.ACTION_POINTS.getValue(fighter))
    }

    private fun canMove(currentFighter: Fighter) = aiComplement.canMove(currentFighter)
        && lastOperation !is MoveOperation
        && getTotalEvadeRatio(currentFighter) >= 1f

    private fun getTotalEvadeRatio(fighter: Fighter): Float = getNeighborEnemies(fighter)
        .map { getEvadeRatio(it, fighter) }
        .reduceOrNull(Float::times)
        ?: 1f

    private fun getEvadeRatio(tacklingFighter: Fighter, playerFighter: Fighter): Float {
        val tackle = DofusCharacteristics.TACKLE_BLOCK.getValue(tacklingFighter).toFloat() +
            DofusCharacteristics.AGILITY.getValue(tacklingFighter).toFloat() / 10f
        val evasion = DofusCharacteristics.TACKLE_EVADE.getValue(playerFighter).toFloat() +
            DofusCharacteristics.AGILITY.getValue(playerFighter).toFloat() / 10f
        return (evasion + 2f) / (2f * (tackle + 2f))
    }

    private fun canCastSpellOnTarget(
        currentFighter: Fighter,
        spell: DofusSpellLevel,
        losCalculator: LosCalculator,
        targetCellId: Int,
        cooldownState: CooldownState,
    ): Boolean {
        val fighter = fightBoard.getFighter(targetCellId)
        if (spell.needTakenCell && fighter == null || spell.needFreeCell && fighter != null) {
            return false
        }
        val turnUseSpellStore = cooldownState.globalTurnUseSpellStore.getTurnUseSpellStore(currentFighter.id)
        if (spell.maxCastPerTarget > 0) {
            fighter?.takeIf { turnUseSpellStore.getUsesOnTarget(spell, it.id) >= spell.maxCastPerTarget }
                ?.let { return false }
        }
        return canCastSpell(currentFighter, spell, losCalculator)
    }

    private fun canCastSpell(
        currentFighter: Fighter,
        spell: DofusSpellLevel,
        losCalculator: LosCalculator,
    ): Boolean = spell.statesCriterion.check(buildCharacterBasicInfo(currentFighter))
        && (!isSpellAttack(spell) || aiComplement.canAttack(currentFighter))
        && (!spell.castTestLos || losCalculator.los)

    private fun buildCharacterBasicInfo(currentFighter: Fighter): DofusCharacterBasicInfo = DofusCharacterBasicInfo(
        characterName = "",
        breedId = -1,
        finishedQuestsIds = emptyList(),
        activeQuestsIds = emptyList(),
        finishedObjectiveIds = emptyList(),
        activeObjectiveIds = emptyList(),
        disabledTransitionZaapMapIds = emptyList(),
        states = currentFighter.stateBuffs.values.map { it.stateId }.toList()
    )

    private fun isSpellAttack(spell: DofusSpellLevel): Boolean = spell.effects.any {
        it.effectType.globalType == DofusSpellEffectGlobalType.ATTACK
    }

    private fun isSpellReady(
        currentFighter: Fighter,
        spell: DofusSpellLevel,
        cooldownState: CooldownState,
        currentFighterAp: Int
    ): Boolean {
        val cooldownSpellStore = cooldownState.globalCooldownSpellStore.getCooldownSpellStore(currentFighter.id)
        val currentCooldown = cooldownSpellStore[spell] ?: 0
        if (currentCooldown > 0) {
            return false
        }
        if (spell.maxCastPerTurn > 0) {
            val turnUseSpellStore = cooldownState.globalTurnUseSpellStore.getTurnUseSpellStore(currentFighter.id)
            if (turnUseSpellStore.getTotalUses(spell) >= spell.maxCastPerTurn) {
                return false
            }
        }
        return currentFighterAp >= spell.apCost
    }

    private fun getNeighborEnemies(currentFighter: Fighter): List<Fighter> {
        val playerPosition = currentFighter.cell
        val enemies = fightBoard.getAllFighters().filter { it.teamId != currentFighter.teamId }
        val neighborsIds = playerPosition.neighbors.map { it.cellId }
        return enemies.filter { neighborsIds.contains(it.cell.cellId) }
    }

    fun makeMove(operation: FightOperation) {
        val currentFighter = getCurrentFighter() ?: error("Couldn't find current fighter")
        val previousEnemyPositions = fightBoard.getEnemyFighters().associateWith { it.cell.cellId }
        when (operation) {
            is MoveOperation -> {
                val cellIds = operation.cellIds
                val distance = cellIds.size
                val currentMp = DofusCharacteristics.MOVEMENT_POINTS.getValue(currentFighter)
                currentFighter.statsById[DofusCharacteristics.MOVEMENT_POINTS.id] = currentMp - distance
                for (cellId in cellIds) {
                    fightBoard.move(currentFighter.id, cellId)
                }
            }
            is CastSpellOperation -> useSpell(currentFighter, operation.spell, operation.targetCellId)
            else -> {}
        }
        val deadFighters = fightBoard.getAllFighters().filter { it.getCurrentHp() <= 0 }
        val refreshAllDanger = deadFighters.isNotEmpty()
        deadFighters.forEach {
            fightBoard.killFighter(it.id)
            dangerMap.remove(it.id)
        }
        fightBoard.getEnemyFighters().forEach {
            if (refreshAllDanger || previousEnemyPositions[it] != it.cell.cellId) {
                dangerMap.recalculateDanger(dofusBoard, fightBoard.deepCopy(), currentFighter, it)
            }
        }
        lastOperation = operation
    }

    private fun useSpell(currentFighter: Fighter, spell: DofusSpellLevel, targetCellId: Int) {
        val target = fightBoard.getFighter(targetCellId)
        val targetId = target?.id ?: Int.MAX_VALUE.toDouble()
        val turnUseSpellStore = cooldownState.globalTurnUseSpellStore.getTurnUseSpellStore(currentFighter.id)
        val cooldownSpellStore = cooldownState.globalCooldownSpellStore.getCooldownSpellStore(currentFighter.id)
        val usesThisTurn = turnUseSpellStore.computeIfAbsent(spell) { HashMap() }
        val usesOnThisTarget = usesThisTurn.computeIfAbsent(targetId) { 0 }
        usesThisTurn[targetId] = usesOnThisTarget + 1
        cooldownSpellStore[spell] = spell.minCastInterval
        val currentAp = DofusCharacteristics.ACTION_POINTS.getValue(currentFighter)
        currentFighter.statsById[DofusCharacteristics.ACTION_POINTS.id] = currentAp - spell.apCost
        spellSimulator.simulateSpell(fightBoard, currentFighter, spell, targetCellId)
    }

    fun evaluate(): Double {
        val allies = fightBoard.getAlliedFighters()
        val realAlliesCount = allies.filter { !it.isSummon() }.size
        val enemies = fightBoard.getEnemyFighters()
        val realEnemiesCount = enemies.filter { !it.isSummon() }.size

        val resultScore = when {
            realEnemiesCount == 0 -> Int.MAX_VALUE
            realAlliesCount == 0 -> Int.MIN_VALUE
            else -> 0
        }
        val alliesHp = allies.sumOf {
            (it.getCurrentHp() * (if (it.isSummon()) 0.1f else 1f)).toInt()
        }
        val enemiesHp = enemies.sumOf {
            (it.getCurrentHp() * (if (it.isSummon()) 0.1f else 1f)).toInt()
        }
        val danger = allies.filter { !it.isSummon() }
            .sumOf { dangerMap.getCellDanger(it.cell.cellId) }

        var distScore = 0
        var apScore = 0
        var mpScore = 0
        val playerFighter = fightBoard.getPlayerFighter()
        if (playerFighter != null) {
            val closestEnemy = fightBoard.getClosestEnemy()
            apScore = DofusCharacteristics.ACTION_POINTS.getValue(playerFighter) * 5
            mpScore = DofusCharacteristics.MOVEMENT_POINTS.getValue(playerFighter)
            if (closestEnemy != null) {
                val dist = dofusBoard.getPathLength(playerFighter.cell, closestEnemy.cell) ?: 1000
                val preferredDist = aiComplement.getIdealDistance(playerFighter)
                distScore = -abs(dist - preferredDist) * 10
            }
        }
        return (realAlliesCount * 3000
            - realEnemiesCount * 2000
            + alliesHp
            - enemiesHp
            - danger
            + distScore
            + mpScore
            + apScore).toDouble() + resultScore
    }
}
