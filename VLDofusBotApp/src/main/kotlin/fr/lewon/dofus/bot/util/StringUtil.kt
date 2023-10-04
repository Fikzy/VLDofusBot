package fr.lewon.dofus.bot.util

import java.text.Normalizer

object StringUtil {

    fun String.removeAccents(): String {
        val temp = Normalizer.normalize(this.lowercase(), Normalizer.Form.NFD)
        val regex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
        return regex.replace(temp, "")
    }
}