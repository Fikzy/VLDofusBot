package fr.lewon.dofus.bot.core.d2o.managers.spell

import fr.lewon.dofus.bot.core.VldbManager
import fr.lewon.dofus.bot.core.criterion.DofusCriterionParser
import fr.lewon.dofus.bot.core.d2o.D2OUtil
import fr.lewon.dofus.bot.core.model.spell.*

object SpellLevelManager : VldbManager {

    private lateinit var spellLevelById: Map<Int, DofusSpellLevel>

    override fun initManager() {
        spellLevelById = D2OUtil.getObjects("SpellLevels").associate {
            val id = it["id"].toString().toInt()
            val spellId = it["spellId"].toString().toInt()
            val criticalHitProbability = it["criticalHitProbability"].toString().toInt()
            val needFreeCell = it["needFreeCell"].toString().toBoolean()
            val needTakenCell = it["needTakenCell"].toString().toBoolean()
            val maxRange = it["range"].toString().toInt()
            val minRange = it["minRange"].toString().toInt()
            val castInLine = it["castInLine"].toString().toBoolean()
            val rangeCanBeBoosted = it["rangeCanBeBoosted"].toString().toBoolean()
            val apCost = it["apCost"].toString().toInt()
            val castInDiagonal = it["castInDiagonal"].toString().toBoolean()
            val initialCooldown = it["initialCooldown"].toString().toInt()
            val castTestLos = it["castTestLos"].toString().toBoolean()
            val minCastInterval = it["minCastInterval"].toString().toInt()
            val maxStack = it["maxStack"].toString().toInt()
            val grade = it["grade"].toString().toInt()
            val minPlayerLevel = it["minPlayerLevel"].toString().toInt()
            val maxCastPerTarget = it["maxCastPerTarget"].toString().toInt()
            val maxCastPerTurn = it["maxCastPerTurn"].toString().toInt()
            val forClientOnly = it["forClientOnly"].toString().toBoolean()
            val unparsedEffects = it["effects"] as List<Map<String, Any>>?
            val effects = parseEffects(unparsedEffects)
            val criticalEffects = parseEffects(it["criticalEffect"] as List<Map<String, Any>>?)
            val isParsedCompletely = unparsedEffects?.size == effects.size
            val statesCriterionStr = it["statesCriterion"].toString()
            val statesCriterion = if (statesCriterionStr != "" && statesCriterionStr != "null") {
                DofusCriterionParser.parse(statesCriterionStr)
            } else DofusCriterionParser.DofusTrueCriterion
            id to DofusSpellLevel(
                id, spellId, criticalHitProbability, needFreeCell, needTakenCell, maxRange, minRange, castInLine,
                rangeCanBeBoosted, apCost, castInDiagonal, initialCooldown, castTestLos, minCastInterval,
                maxStack, grade, minPlayerLevel, maxCastPerTarget, maxCastPerTurn, forClientOnly,
                effects, criticalEffects, isParsedCompletely, statesCriterion
            )
        }
    }

    private fun parseEffects(effectsMaps: List<Map<String, Any>>?): List<DofusSpellEffect> {
        return effectsMaps?.mapNotNull { parseEffect(it) } ?: emptyList()
    }

    private fun parseEffect(effectMap: Map<String, Any>): DofusSpellEffect? {
        val effectId = effectMap["effectId"].toString().toInt()
        val forClientOnly = effectMap["forClientOnly"].toString().toBoolean()
        if (forClientOnly) {
            return null
        }
        val spellEffectType = DofusSpellEffectType.fromEffectId(effectId)
            ?: return null
        val rawZone = effectMap["rawZone"].toString()
        val area = parseEffectArea(rawZone)
            ?: return null
        val targets = DofusSpellTarget.fromString(effectMap["targetMask"].toString())
        val min = effectMap["diceNum"].toString().toInt()
        val max = effectMap["diceSide"].toString().toInt()
        val value = effectMap["value"].toString().toInt()
        return DofusSpellEffect(min, max, value, area, spellEffectType, targets)
    }

    private fun parseEffectArea(effectAreaStr: String): DofusEffectArea? {
        val zoneTypeKey = effectAreaStr.firstOrNull() ?: return null
        val effectZoneType = DofusEffectAreaType.fromKey(zoneTypeKey) ?: return null
        val size = if (effectAreaStr.length > 1) {
            //TODO parse all the effect zone params
            effectAreaStr[1].digitToIntOrNull() ?: return null
        } else 1
        return DofusEffectArea(effectZoneType, size)
    }

    override fun getNeededManagers(): List<VldbManager> {
        return emptyList()
    }

    fun getSpellLevel(id: Int): DofusSpellLevel {
        return spellLevelById[id] ?: error("No spell for id : $id")
    }

}