package fr.lewon.dofus.bot.game.fight.ai.impl

import fr.lewon.dofus.bot.game.DofusBoard
import fr.lewon.dofus.bot.game.DofusCell
import fr.lewon.dofus.bot.game.fight.FightBoard
import fr.lewon.dofus.bot.game.fight.ai.FightAI
import fr.lewon.dofus.bot.game.fight.ai.FightState
import fr.lewon.dofus.bot.game.fight.ai.complements.AIComplement
import fr.lewon.dofus.bot.game.fight.operations.FightOperation
import fr.lewon.dofus.bot.game.fight.operations.PassTurnOperation
import fr.lewon.dofus.bot.util.io.WaitUtil
import kotlin.math.abs
import kotlin.random.Random

open class DefaultFightAI(dofusBoard: DofusBoard, aiComplement: AIComplement) : FightAI(dofusBoard, aiComplement) {

    override fun selectStartCell(fightBoard: FightBoard): DofusCell? {
        val tempFightBoard = fightBoard.deepCopy()
        val playerFighter = tempFightBoard.getPlayerFighter() ?: error("Couldn't find player fighter")
        val idealDist = aiComplement.getIdealDistance(playerFighter)
        return dofusBoard.startCells.map {
            val tempPlayerFighter = tempFightBoard.getPlayerFighter()
                ?: error("Player fighter not found")
            tempFightBoard.move(tempPlayerFighter, it)
            val closestEnemy = tempFightBoard.getClosestEnemy() ?: error("Closest enemy not found")
            it to (dofusBoard.getPathLength(it, closestEnemy.cell)
                ?: dofusBoard.getDist(it, closestEnemy.cell))
        }.minByOrNull { abs(idealDist - it.second) }?.first
    }

    override fun doGetNextOperation(fightBoard: FightBoard, initialState: FightState): FightOperation {
        val initialNode = Node(initialState.deepCopy(), ArrayList(), initialState.evaluate())

        var bestNode = initialNode
        val frontier = mutableListOf(initialNode)
        val startTime = System.currentTimeMillis()

        val maxTimeMillis = 1800
        val minTimeMillis = 800
        var newPopulationMultiplier = 3
        while (frontier.isNotEmpty() && System.currentTimeMillis() - startTime < maxTimeMillis) {
            val nodesToExplore = if (frontier.size < 100) {
                frontier.toList()
            } else {
                selectNodesToExplore(frontier, (newPopulationMultiplier++) * 100)
            }
            for (node in nodesToExplore) {
                frontier.remove(node)
                if (System.currentTimeMillis() - startTime > maxTimeMillis) {
                    return selectOperation(bestNode)
                }
                for (move in node.state.getPossibleOperations()) {
                    val childState = node.state.deepCopy()
                    val isPassTurn = move == PassTurnOperation
                    if (!isPassTurn) {
                        childState.makeMove(move)
                    }
                    val operations = ArrayList(node.operations).also { it.add(move) }
                    val childNodeScore = if (isPassTurn) node.score else childState.evaluate()
                    val childNode = Node(childState, operations, childNodeScore)
                    if (!isPassTurn) {
                        frontier.add(childNode)
                    }
                    if (bestNode.score < childNodeScore) {
                        bestNode = childNode
                        if (System.currentTimeMillis() - startTime > minTimeMillis && bestNode.score >= Int.MAX_VALUE.toDouble()) {
                            return selectOperation(bestNode)
                        }
                    }
                }
            }
        }
        WaitUtil.waitUntil { System.currentTimeMillis() - startTime > minTimeMillis }
        return selectOperation(bestNode)
    }

    private fun selectOperation(node: Node): FightOperation {
        return node.operations.firstOrNull() ?: PassTurnOperation
    }

    private fun selectNodesToExplore(frontierNodes: List<Node>, newPopulationSize: Int): List<Node> {
        val minScore = frontierNodes.minOfOrNull { it.score }
            ?: return emptyList()
        val relativeFitnessByIndividual = frontierNodes.associateWith { it.score + 800 - minScore }
        val fitnessSum = relativeFitnessByIndividual.values.sum()
        val nodesToExplore = ArrayList<Node>()
        for (i in 0 until newPopulationSize) {
            val node = selectIndividualRoulette(relativeFitnessByIndividual, fitnessSum)
            if (!nodesToExplore.contains(node)) {
                nodesToExplore.add(node)
            }
        }
        return nodesToExplore
    }

    private fun selectIndividualRoulette(
        adaptedScoreByNode: Map<Node, Double>,
        fitnessSum: Double
    ): Node {
        var partialFitnessSum = 0.0
        val randomDouble = Random.nextDouble() * fitnessSum
        val entries = ArrayList<Map.Entry<Node, Double>>(adaptedScoreByNode.entries)
        for (i in adaptedScoreByNode.entries.indices.reversed()) {
            partialFitnessSum += entries[i].value
            if (partialFitnessSum >= randomDouble) {
                return entries[i].key
            }
        }
        error("Failed to select a node")
    }

    private class Node(val state: FightState, val operations: List<FightOperation>, val score: Double)
}