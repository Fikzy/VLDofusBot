package fr.lewon.dofus.bot.core.ui.geometry.ui

import fr.lewon.dofus.bot.core.ui.geometry.GameRectangle

class UIRectangle(position: UIPoint, size: UIPoint) : GameRectangle<UIPoint>(position, size) {

    override fun buildPoint(x: Float, y: Float): UIPoint = UIPoint(x, y)
}