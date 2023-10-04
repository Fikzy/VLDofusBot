package fr.lewon.dofus.bot.core.model.spell

import fr.lewon.dofus.bot.core.fighter.IDofusFighter

data class DofusSpellEffect(
    var min: Int,
    var max: Int,
    var value: Int,
    var area: DofusEffectArea,
    var effectType: DofusSpellEffectType,
    var targets: List<DofusSpellTarget>
) {

    private val primarySpellTargets = targets.filter { it.type.isPrimary }
    private val secondarySpellTargets = targets.filter { !it.type.isPrimary }

    fun canHitTarget(caster: IDofusFighter, target: IDofusFighter): Boolean {
        return primarySpellTargets.any { primarySpellTarget ->
            primarySpellTarget.isTargetValid(caster, target) && secondarySpellTargets.all { secondarySpellTarget ->
                secondarySpellTarget.isTargetValid(caster, target)
            }
        }
    }
}