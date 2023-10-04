package fr.lewon.dofus.bot.gui.main.devtools.fight

import androidx.compose.runtime.mutableStateOf
import fr.lewon.dofus.bot.core.d2o.managers.characteristic.BreedManager
import fr.lewon.dofus.bot.core.d2o.managers.spell.SpellVariantManager
import fr.lewon.dofus.bot.core.d2p.maps.D2PMapsAdapter
import fr.lewon.dofus.bot.core.model.charac.DofusBreed
import fr.lewon.dofus.bot.core.model.spell.DofusSpell
import fr.lewon.dofus.bot.game.DofusCell
import fr.lewon.dofus.bot.game.fight.DofusCharacteristics
import fr.lewon.dofus.bot.game.fight.FightBoard
import fr.lewon.dofus.bot.game.fight.ai.SpellSimulator
import fr.lewon.dofus.bot.game.fight.fighter.Fighter
import fr.lewon.dofus.bot.game.fight.utils.FightSpellUtils
import fr.lewon.dofus.bot.game.fight.utils.FighterInfoInitializer
import fr.lewon.dofus.bot.gui.ComposeUIUtil
import fr.lewon.dofus.bot.model.characters.DofusCharacter
import fr.lewon.dofus.bot.sniffer.model.types.game.character.characteristic.CharacterCharacteristicValue
import fr.lewon.dofus.bot.sniffer.model.types.game.context.EntityDispositionInformations
import fr.lewon.dofus.bot.sniffer.model.types.game.context.fight.GameContextBasicSpawnInformation
import fr.lewon.dofus.bot.sniffer.model.types.game.context.fight.GameFightCharacteristics
import fr.lewon.dofus.bot.sniffer.model.types.game.context.fight.GameFightFighterNamedInformations
import fr.lewon.dofus.bot.sniffer.model.types.game.look.EntityLook
import fr.lewon.dofus.bot.util.network.info.GameInfo
import java.util.*
import java.util.concurrent.atomic.AtomicLong

object FightDevToolsUiUtil : ComposeUIUtil() {

    const val AllyTeamId = 1
    const val EnemyTeamId = 2

    private val nextFighterId = AtomicLong(1L)

    val gameInfo = mutableStateOf(GameInfo(DofusCharacter("[TEST_CHARACTER]")))
    val fightBoard = mutableStateOf(FightBoard(gameInfo.value))
    val currentMapId = mutableStateOf(88212247.0)
    val mapIdError = mutableStateOf("")
    val dofusBoard = gameInfo.value.dofusBoard
    val selectedFightBoardTool = mutableStateOf(FightBoardTools.PlaceCharacter)
    val playerFighter = mutableStateOf<Fighter?>(null)

    val currentBreed = mutableStateOf(BreedManager.getAllBreeds().first())
    val currentSpells = mutableStateOf(emptyList<DofusSpell>())
    val currentSpell = mutableStateOf<DofusSpell?>(null)
    val targetCellIds = mutableStateOf(emptyList<Int>())

    init {
        updateDofusBoard(currentMapId.value)
        updateBreed(currentBreed.value)
    }

    fun skipTurn() {
        fightBoard.value.triggerNewTurn()
    }

    fun updateDofusBoard(mapId: Double) {
        try {
            val mapData = D2PMapsAdapter.getMapData(mapId)
            dofusBoard.updateCells(mapData.completeCellDataByCellId.values.map { it.cellData })
            currentMapId.value = mapId
            mapIdError.value = ""
            updateTargetCells()
        } catch (e: Exception) {
            mapIdError.value = "Couldn't load map [${mapId.toLong()}], are you sure that it exists?"
        }
    }

    fun updateBreed(breed: DofusBreed) {
        currentBreed.value = breed
        val spells = SpellVariantManager.getSortedSpells(currentBreed.value.id)
            .plus(SpellVariantManager.getSortedSpells(BreedManager.anyBreedId))
        currentSpells.value = spells
        currentSpell.value = spells.first()
    }

    fun updateSpell(spell: DofusSpell) {
        currentSpell.value = spell
        updateTargetCells()
    }

    private fun updateTargetCells() {
        val characterFighter = playerFighter.value
        val spell = currentSpell.value
        val spellLevel = spell?.levels?.lastOrNull()
        targetCellIds.value = if (characterFighter == null || spellLevel == null) {
            emptyList()
        } else FightSpellUtils.getRawSpellTargetCells(
            dofusBoard = dofusBoard,
            casterFighter = characterFighter,
            spell = spellLevel
        ).map { it.cellId }
    }

    fun castSpell(cell: DofusCell) {
        if (cell.cellId in targetCellIds.value) {
            val caster = playerFighter.value
            val spell = currentSpell.value
            val spellLevel = spell?.levels?.lastOrNull()
            if (caster != null && spellLevel != null) {
                SpellSimulator(dofusBoard).simulateSpell(fightBoard.value, caster, spellLevel, cell.cellId)
            }
        }
        updateTargetCells()
    }

    fun placeCharacter(cell: DofusCell) {
        playerFighter.value?.let {
            fightBoard.value.killFighter(it.id)
        }
        playerFighter.value = placeFighter(cell, AllyTeamId)
        updateTargetCells()
    }

    fun placeAlly(cell: DofusCell) = placeFighter(cell, AllyTeamId)

    fun placeEnemy(cell: DofusCell) = placeFighter(cell, EnemyTeamId)

    private fun placeFighter(cell: DofusCell, teamId: Int): Fighter? {
        val fightBoardValue = fightBoard.value
        if (cell.isAccessible()) {
            val fighterHere = fightBoardValue.getFighter(cell)
            if (fighterHere != null) {
                fightBoardValue.killFighter(fighterHere.id)
            }
            val fighterInfo = GameFightFighterNamedInformations().also { fighterInfo ->
                FighterInfoInitializer.initGameFightFighterNamedInformations(
                    fighterInformation = fighterInfo,
                    contextualId = nextFighterId.getAndIncrement().toDouble(),
                    disposition = EntityDispositionInformations().also {
                        it.cellId = cell.cellId
                    },
                    look = EntityLook(),
                    spawnInfo = GameContextBasicSpawnInformation().also {
                        it.teamId = teamId
                    },
                    wave = 0,
                    stats = GameFightCharacteristics(),
                    previousPositions = ArrayList(),
                    UUID.randomUUID().toString()
                )
            }
            val newFighter = fightBoardValue.createOrUpdateFighter(fighterInfo)
            val characteristics = listOf(
                CharacterCharacteristicValue().also {
                    it.characteristicId = DofusCharacteristics.ACTION_POINTS.id
                    it.total = 12
                },
                CharacterCharacteristicValue().also {
                    it.characteristicId = DofusCharacteristics.MOVEMENT_POINTS.id
                    it.total = 6
                }
            )
            fightBoardValue.updateFighterCharacteristics(newFighter, characteristics)
            newFighter.maxHp = 1000
            newFighter.baseHp = 1000
            updateTargetCells()
            return newFighter
        }
        return null
    }

    fun removeFighter(cell: DofusCell) {
        val fightBoard = FightDevToolsUiUtil.fightBoard.value
        fightBoard.getFighter(cell)?.let { fighter ->
            fightBoard.killFighter(fighter.id)
        }
        updateTargetCells()
    }

}