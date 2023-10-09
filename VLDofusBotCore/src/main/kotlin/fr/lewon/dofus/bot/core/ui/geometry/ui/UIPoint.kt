package fr.lewon.dofus.bot.core.ui.geometry.ui

import fr.lewon.dofus.bot.core.ui.geometry.GamePoint

class UIPoint(x: Float = 0f, y: Float = 0f) : GamePoint<UIPoint>(x, y) {

    override fun buildPoint(x: Float, y: Float): UIPoint = UIPoint(x, y)
}