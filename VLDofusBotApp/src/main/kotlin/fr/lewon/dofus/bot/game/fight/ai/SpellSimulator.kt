package fr.lewon.dofus.bot.game.fight.ai

import fr.lewon.dofus.bot.core.d2o.managers.spell.SpellManager
import fr.lewon.dofus.bot.core.model.spell.DofusSpellEffect
import fr.lewon.dofus.bot.core.model.spell.DofusSpellEffectType
import fr.lewon.dofus.bot.core.model.spell.DofusSpellLevel
import fr.lewon.dofus.bot.core.model.spell.DofusSpellTargetType
import fr.lewon.dofus.bot.game.DofusBoard
import fr.lewon.dofus.bot.game.DofusCell
import fr.lewon.dofus.bot.game.fight.DofusCharacteristics
import fr.lewon.dofus.bot.game.fight.FightBoard
import fr.lewon.dofus.bot.game.fight.fighter.Fighter
import fr.lewon.dofus.bot.game.fight.utils.FightSpellAreaUtils
import fr.lewon.dofus.bot.game.fight.utils.FighterInfoInitializer
import fr.lewon.dofus.bot.sniffer.model.types.game.context.EntityDispositionInformations
import fr.lewon.dofus.bot.sniffer.model.types.game.context.fight.GameContextBasicSpawnInformation
import fr.lewon.dofus.bot.sniffer.model.types.game.context.fight.GameFightCharacteristics
import fr.lewon.dofus.bot.sniffer.model.types.game.context.fight.GameFightMonsterInformations
import fr.lewon.dofus.bot.sniffer.model.types.game.look.EntityLook
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

class SpellSimulator(val dofusBoard: DofusBoard) {

    private val damageCalculator = DamageCalculator()

    fun simulateSpell(
        fightBoard: FightBoard,
        caster: Fighter,
        spell: DofusSpellLevel,
        targetCellId: Int,
        isSubSpell: Boolean = false
    ) {
        if (!isSubSpell) {
            fightBoard.getAllFighters(true).forEach {
                it.telefraggedThisTurn = false
            }
        }
        val criticalChance = spell.criticalHitProbability + DofusCharacteristics.CRITICAL_HIT.getValue(caster)
        val criticalHit = spell.criticalEffects.isNotEmpty() && criticalChance >= 95
        val effects = if (criticalHit) spell.criticalEffects else spell.effects
        val mainTarget = fightBoard.getFighter(targetCellId)
        effects.forEach {
            val effectTargetCellId = when {
                isCasterTarget(it) -> caster.cell.cellId
                mainTarget != null -> mainTarget.cell.cellId
                else -> targetCellId
            }
            simulateEffect(fightBoard, caster, it, effectTargetCellId, criticalHit)
        }
    }

    private fun isCasterTarget(effect: DofusSpellEffect): Boolean {
        val targetTypes = effect.targets.map { it.type }
        return DofusSpellTargetType.CASTER_1 in targetTypes || DofusSpellTargetType.CASTER_2 in targetTypes
    }

    private fun simulateEffect(
        fightBoard: FightBoard,
        caster: Fighter,
        effect: DofusSpellEffect,
        targetCellId: Int,
        criticalHit: Boolean
    ) {
        val casterCellId = caster.cell.cellId
        val targetCell = dofusBoard.getCell(targetCellId)
        val affectedCellIds =
            FightSpellAreaUtils.getAffectedCells(dofusBoard, fightBoard, casterCellId, targetCellId, effect.area)
        val fightersInAOE = affectedCellIds.mapNotNull { fightBoard.getFighter(it) }
            .filter { effect.canHitTarget(caster, it) }
        when (effect.effectType) {
            DofusSpellEffectType.MP_BUFF ->
                simulateBuff(fightersInAOE, DofusCharacteristics.MOVEMENT_POINTS, effect.min)
            DofusSpellEffectType.AP_BUFF ->
                simulateBuff(fightersInAOE, DofusCharacteristics.ACTION_POINTS, effect.min)
            DofusSpellEffectType.CRITICAL_BUFF ->
                simulateBuff(fightersInAOE, DofusCharacteristics.CRITICAL_HIT, effect.min)
            DofusSpellEffectType.DAMAGE_BUFF ->
                simulateBuff(fightersInAOE, DofusCharacteristics.ALL_DAMAGES_BONUS, effect.min)
            DofusSpellEffectType.POWER_BUFF ->
                simulateBuff(fightersInAOE, DofusCharacteristics.DAMAGES_BONUS_PERCENT, effect.min)
            DofusSpellEffectType.DASH ->
                simulateDash(fightBoard, caster, targetCell, effect.min)
            DofusSpellEffectType.TELEPORT ->
                simulateTeleport(fightBoard, caster, targetCellId)
            DofusSpellEffectType.PUSH ->
                simulatePush(fightBoard, caster, targetCellId, fightersInAOE, effect.min)
            DofusSpellEffectType.PULL ->
                simulatePull(fightBoard, casterCellId, targetCellId, fightersInAOE, effect.min)
            DofusSpellEffectType.SWITCH_POSITIONS ->
                simulateSwitchPositions(fightBoard, caster, fightersInAOE)
            DofusSpellEffectType.ROLLBACK_PREVIOUS_POSITION ->
                simulateRollbackPreviousPosition(fightBoard, fightersInAOE)
            DofusSpellEffectType.TELESWAP_MIRROR ->
                simulateTeleswapMirror(fightBoard, caster, fightersInAOE)
            DofusSpellEffectType.TELESWAP_MIRROR_CASTER ->
                simulateTeleswapMirrorCaster(fightBoard, caster, fightersInAOE)
            DofusSpellEffectType.AIR_DAMAGE, DofusSpellEffectType.EARTH_DAMAGE, DofusSpellEffectType.FIRE_DAMAGE, DofusSpellEffectType.NEUTRAL_DAMAGE, DofusSpellEffectType.WATER_DAMAGE ->
                simulateDamages(caster, targetCell, fightersInAOE, effect, criticalHit)
            DofusSpellEffectType.MP_DECREASED_EARTH_DAMAGE ->
                simulateMpDecreasedDamages(caster, targetCell, fightersInAOE, effect, criticalHit)
            DofusSpellEffectType.BEST_ELEMENT_DAMAGE ->
                simulateBestElementDamages(caster, targetCell, fightersInAOE, effect, criticalHit)
            DofusSpellEffectType.WORST_ELEMENT_DAMAGE ->
                simulateWorstElementDamages(caster, targetCell, fightersInAOE, effect, criticalHit)
            DofusSpellEffectType.AIR_LIFE_STEAL, DofusSpellEffectType.EARTH_LIFE_STEAL, DofusSpellEffectType.FIRE_LIFE_STEAL, DofusSpellEffectType.NEUTRAL_LIFE_STEAL, DofusSpellEffectType.WATER_LIFE_STEAL ->
                simulateLifeSteal(caster, targetCell, fightersInAOE, effect, criticalHit)
            DofusSpellEffectType.CAST_SUB_SPELL_ON_TARGET ->
                simulateSubSpell(fightBoard, caster, fightersInAOE, effect)
            DofusSpellEffectType.CAST_SUB_SPELL_ON_CASTER_GLOBAL_LIMITATION ->
                simulateSubSpell(fightBoard, caster, fightersInAOE, effect)
            DofusSpellEffectType.ADD_STATE ->
                simulateAddState(fightersInAOE, effect)
            DofusSpellEffectType.REMOVE_STATE ->
                simulateRemoveState(fightersInAOE, effect)
            DofusSpellEffectType.SUMMON_CREATURE ->
                simulateSummonCreature(fightBoard, caster, targetCell, effect)
            else -> Unit
        }
    }

    private fun simulateSummonCreature(
        fightBoard: FightBoard,
        caster: Fighter,
        targetCell: DofusCell,
        effect: DofusSpellEffect
    ) {
        val fighterInfo = GameFightMonsterInformations().also { monsterInfo ->
            val genericId = effect.min
            val level = 1
            val disposition = EntityDispositionInformations().also {
                it.cellId = targetCell.cellId
            }
            val summonInfo = GameContextBasicSpawnInformation().also {
                it.teamId = caster.teamId
            }
            val stats = GameFightCharacteristics().also {
                it.summoner = caster.id
                it.summoned = true
            }
            FighterInfoInitializer.initGameFightMonsterInformations(
                fighterInformation = monsterInfo,
                contextualId = Math.random() * 1_000_000,
                disposition = disposition,
                look = EntityLook(),
                spawnInfo = summonInfo,
                wave = 0,
                stats = stats,
                previousPositions = ArrayList(),
                creatureGenericId = genericId,
                creatureGrade = 0,
                creatureLevel = level
            )
        }
        val summonFighter = fightBoard.createOrUpdateFighter(fighterInfo)
        summonFighter.maxHp = 1
        summonFighter.baseHp = 1
    }

    private fun simulateRemoveState(fightersInAOE: List<Fighter>, effect: DofusSpellEffect) {
        for (fighter in fightersInAOE) {
            val toRemoveKeys = fighter.stateBuffs.filter { it.value.stateId == effect.value }.map { it.key }
            for (toRemoveKey in toRemoveKeys) {
                fighter.stateBuffs.remove(toRemoveKey)
            }
        }
    }

    private fun simulateAddState(fightersInAOE: List<Fighter>, effect: DofusSpellEffect) {
        for (fighter in fightersInAOE) {
            fighter.addStateBuff(UUID.randomUUID().toString(), 1, effect.value)
        }
    }

    private fun simulateSubSpell(
        fightBoard: FightBoard,
        caster: Fighter,
        affectedFighters: List<Fighter>,
        effect: DofusSpellEffect,
    ) {
        val subSpellLevel = SpellManager.getSpell(effect.min)?.levels?.get(effect.max - 1)
        if (subSpellLevel != null) {
            for (fighter in affectedFighters) {
                this.simulateSpell(fightBoard, caster, subSpellLevel, fighter.cell.cellId, true)
            }
        }
    }

    private fun simulateLifeSteal(
        caster: Fighter,
        aoeCenter: DofusCell,
        fightersInAOE: List<Fighter>,
        effect: DofusSpellEffect,
        criticalHit: Boolean
    ) {
        var dealtDamagesTotal = 0
        for (fighter in fightersInAOE) {
            val distToCenter = dofusBoard.getDist(fighter.cell, aoeCenter)
            val rawDamage = damageCalculator.getRealEffectDamage(effect, caster, fighter, criticalHit).minDamage
            val realDamage = (rawDamage.toFloat() * (1f - 0.1f * distToCenter)).toInt()
            dealtDamagesTotal += minOf(fighter.getCurrentHp(), realDamage)
            fighter.hpLost += realDamage
        }
        val maxHeal = caster.maxHp - caster.getCurrentHp()
        val heal = max(maxHeal, dealtDamagesTotal / 2)
        caster.hpHealed += heal
    }

    private fun simulateDamages(
        caster: Fighter,
        aoeCenter: DofusCell,
        fightersInAOE: List<Fighter>,
        effect: DofusSpellEffect,
        criticalHit: Boolean
    ) {
        for (fighter in fightersInAOE) {
            val distToCenter = dofusBoard.getDist(fighter.cell, aoeCenter)
            val rawDamage = damageCalculator.getRealEffectDamage(effect, caster, fighter, criticalHit).minDamage
            val realDamage = (rawDamage.toFloat() * (1f - 0.1f * distToCenter)).toInt()
            fighter.hpLost += realDamage
        }
    }

    private fun simulateMpDecreasedDamages(
        caster: Fighter,
        aoeCenter: DofusCell,
        fightersInAOE: List<Fighter>,
        effect: DofusSpellEffect,
        criticalHit: Boolean
    ) {
        val totalMp = caster.totalMp
        val mpUsed = caster.totalMp - DofusCharacteristics.MOVEMENT_POINTS.getValue(caster)
        if (totalMp > 0) {
            val mpUsedRatio = min(1f, max(0f, (totalMp - mpUsed).toFloat() / totalMp.toFloat()))
            for (fighter in fightersInAOE) {
                val distToCenter = dofusBoard.getDist(fighter.cell, aoeCenter)
                val rawDamage = damageCalculator.getRealEffectDamage(effect, caster, fighter, criticalHit).minDamage
                val realDamage = (rawDamage.toFloat() * mpUsedRatio * (1f - 0.1f * distToCenter)).toInt()
                fighter.hpLost += realDamage
            }
        }
    }

    private fun simulateBestElementDamages(
        caster: Fighter,
        aoeCenter: DofusCell,
        fightersInAOE: List<Fighter>,
        effect: DofusSpellEffect,
        criticalHit: Boolean
    ) {
        for (fighter in fightersInAOE) {
            val distToCenter = dofusBoard.getDist(fighter.cell, aoeCenter)
            val damage = listOf(
                DofusSpellEffectType.AIR_DAMAGE,
                DofusSpellEffectType.EARTH_DAMAGE,
                DofusSpellEffectType.FIRE_DAMAGE,
                DofusSpellEffectType.NEUTRAL_DAMAGE,
                DofusSpellEffectType.WATER_DAMAGE
            ).maxOf {
                damageCalculator.getRealEffectDamage(
                    effect.copy(effectType = it),
                    caster,
                    fighter,
                    criticalHit
                ).minDamage
            }
            val realDamage = (damage.toFloat() * (1f - 0.1f * distToCenter)).toInt()
            fighter.hpLost += realDamage
        }
    }

    private fun simulateWorstElementDamages(
        caster: Fighter,
        aoeCenter: DofusCell,
        fightersInAOE: List<Fighter>,
        effect: DofusSpellEffect,
        criticalHit: Boolean
    ) {
        for (fighter in fightersInAOE) {
            val distToCenter = dofusBoard.getDist(fighter.cell, aoeCenter)
            val damage = listOf(
                DofusSpellEffectType.AIR_DAMAGE,
                DofusSpellEffectType.EARTH_DAMAGE,
                DofusSpellEffectType.FIRE_DAMAGE,
                DofusSpellEffectType.NEUTRAL_DAMAGE,
                DofusSpellEffectType.WATER_DAMAGE
            ).minOf {
                damageCalculator.getRealEffectDamage(
                    effect.copy(effectType = it),
                    caster,
                    fighter,
                    criticalHit
                ).minDamage
            }
            val realDamage = (damage.toFloat() * (1f - 0.1f * distToCenter)).toInt()
            fighter.hpLost += realDamage
        }
    }

    private fun simulateSwitchPositions(fightBoard: FightBoard, caster: Fighter, fightersInAOE: List<Fighter>) {
        for (target in fightersInAOE) {
            val oldCasterCellId = caster.cell.cellId
            fightBoard.move(caster, target.cell.cellId)
            fightBoard.move(target, oldCasterCellId)
        }
    }

    private fun simulateRollbackPreviousPosition(fightBoard: FightBoard, fightersInAOE: List<Fighter>) {
        for (fighter in fightersInAOE.filter { it.previousCellIds.isNotEmpty() }) {
            val newPosition = fighter.previousCellIds.lastOrNull()
            if (newPosition != null) {
                val fighterOnNewCell = fightBoard.getFighter(newPosition)
                if (fighterOnNewCell != null && fighterOnNewCell != fighter) {
                    fightBoard.move(fighterOnNewCell, fighter.cell)
                    fighterOnNewCell.telefraggedThisTurn = true
                    fighter.telefraggedThisTurn = true
                }
                fightBoard.move(fighter, newPosition, true)
            }
        }
    }

    private fun simulateTeleswapMirror(
        fightBoard: FightBoard,
        caster: Fighter,
        fightersInAOE: List<Fighter>
    ) {
        for (fighter in fightersInAOE) {
            val targetCell = fighter.cell
            val dRow = targetCell.row - caster.cell.row
            val dCol = targetCell.col - caster.cell.col
            dofusBoard.getCell(targetCell.col + dCol, targetCell.row + dRow)?.let { newCasterCell ->
                if (newCasterCell.isAccessible()) {
                    val fighterOnNewCell = fightBoard.getFighter(newCasterCell)
                    if (fighterOnNewCell != null) {
                        fightBoard.move(fighterOnNewCell, caster.cell)
                        fighterOnNewCell.telefraggedThisTurn = true
                    }
                    fightBoard.move(caster, newCasterCell)
                }
            }
        }
    }

    private fun simulateTeleswapMirrorCaster(
        fightBoard: FightBoard,
        caster: Fighter,
        fightersInAOE: List<Fighter>
    ) {
        for (fighter in fightersInAOE) {
            val dRow = fighter.cell.row - caster.cell.row
            val dCol = fighter.cell.col - caster.cell.col
            dofusBoard.getCell(caster.cell.col - dCol, caster.cell.row - dRow)?.let { newTargetCell ->
                if (newTargetCell.isAccessible()) {
                    val fighterOnNewCell = fightBoard.getFighter(newTargetCell)
                    if (fighterOnNewCell != null) {
                        fightBoard.move(fighterOnNewCell, fighter.cell)
                        fighterOnNewCell.telefraggedThisTurn = true
                        fighter.telefraggedThisTurn = true
                    }
                    fightBoard.move(fighter, newTargetCell)
                }
            }
        }
    }

    private fun simulateTeleport(fightBoard: FightBoard, caster: Fighter, targetCellId: Int) {
        fightBoard.move(caster.id, targetCellId)
    }

    private fun simulatePull(
        fightBoard: FightBoard,
        casterCellId: Int,
        targetCellId: Int,
        fightersInAOE: List<Fighter>,
        amount: Int
    ) {
        for (fighter in fightersInAOE) {
            val pullTowardCellId = getPushOriginCellId(casterCellId, targetCellId, fighter.cell.cellId)
            val pullTowardCell = dofusBoard.getCell(pullTowardCellId)
            val pullDest = getRealDashDest(fightBoard, amount, fighter.cell, pullTowardCell)
            fightBoard.move(fighter, pullDest)
        }
    }

    private fun simulatePush(
        fightBoard: FightBoard,
        caster: Fighter,
        targetCellId: Int,
        fightersInAOE: List<Fighter>,
        amount: Int
    ) {
        for (fighter in fightersInAOE) {
            val pushFromCellId = getPushOriginCellId(caster.cell.cellId, targetCellId, fighter.cell.cellId)
            val pushFromCell = dofusBoard.getCell(pushFromCellId)
            val pushDest = getRealDashDest(fightBoard, amount, fighter.cell, pushFromCell, true)
            val oldLoc = fighter.cell.cellId
            fightBoard.move(fighter, pushDest)
            val pushedDist = dofusBoard.getDist(oldLoc, fighter.cell.cellId)
            val doPouAmount = max(0, amount - pushedDist)
            if (doPouAmount > 0) {
                val level = DofusCharacteristics.LEVEL.getValue(caster).toFloat()
                val doPou = DofusCharacteristics.PUSH_DAMAGE_BONUS.getValue(caster).toFloat()
                val rePou = DofusCharacteristics.PUSH_DAMAGE_REDUCTION.getValue(fighter).toFloat()
                val pushDamage = ((level / 2f + doPou - rePou + 32f) * doPouAmount.toFloat() / 4f).toInt()
                fighter.hpLost += pushDamage
            }
        }
    }

    private fun getPushOriginCellId(
        casterCellId: Int,
        spellTargetCellId: Int,
        hitFighterCellId: Int,
    ): Int {
        return if (spellTargetCellId == hitFighterCellId) {
            casterCellId
        } else {
            spellTargetCellId
        }
    }

    private fun simulateDash(fightBoard: FightBoard, caster: Fighter, targetCell: DofusCell, amount: Int) {
        val dashDest = getRealDashDest(fightBoard, amount, caster.cell, targetCell)
        fightBoard.move(caster.id, dashDest.cellId)
    }

    private fun simulateBuff(affectedFighters: List<Fighter>, characteristics: DofusCharacteristics, amount: Int) {
        for (fighter in affectedFighters) {
            val current = characteristics.getValue(fighter)
            fighter.statsById[characteristics.id] = current + amount
        }
    }

    private fun getRealDashDest(
        fightBoard: FightBoard,
        amount: Int,
        fromCell: DofusCell,
        toCell: DofusCell,
        invertDirection: Boolean = false
    ): DofusCell {
        val invertProduct = if (invertDirection) -1 else 1
        val dCol = toCell.col - fromCell.col
        val dRow = toCell.row - fromCell.row
        val sDCol = dCol.sign * invertProduct
        val sDRow = dRow.sign * invertProduct
        val absDCol = abs(dCol)
        val absDRow = abs(dRow)
        var destCell = fromCell
        val realAmount = if (absDCol == absDRow) amount / 2 else amount
        for (i in 0 until realAmount) {
            if (destCell.cellId == toCell.cellId) {
                break
            }
            val newDestCell = if (absDCol > absDRow) {
                dofusBoard.getCell(destCell.col + sDCol, destCell.row)
            } else if (absDCol < absDRow) {
                dofusBoard.getCell(destCell.col, destCell.row + sDRow)
            } else {
                val alignedCell1 =
                    dofusBoard.getCell(destCell.col + sDCol, destCell.row)
                val alignedCell2 =
                    dofusBoard.getCell(destCell.col, destCell.row + sDRow)
                if (alignedCell1 != null && alignedCell2 != null
                    && alignedCell1.isAccessible() && alignedCell2.isAccessible()
                    && !fightBoard.isFighterHere(alignedCell1) && !fightBoard.isFighterHere(alignedCell2)
                ) {
                    dofusBoard.getCell(destCell.col + sDCol, destCell.row + sDRow)
                } else {
                    null
                }
            }
            destCell = newDestCell?.takeIf { it.isAccessible() && !fightBoard.isFighterHere(it) }
                ?: break
        }
        return destCell
    }

}