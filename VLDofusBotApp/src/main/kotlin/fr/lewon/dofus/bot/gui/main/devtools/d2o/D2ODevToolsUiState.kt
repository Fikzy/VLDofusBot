package fr.lewon.dofus.bot.gui.main.devtools.d2o

import androidx.compose.foundation.lazy.LazyListState

data class D2ODevToolsUiState(
    val selectedD2OModule: String? = null,
    val loading: Boolean = false,
    val selectedModuleListState: LazyListState = LazyListState(),
    val nameFilter: String = "",
    val moduleItemNameFilter: String = "",
    val moduleItemIdFilter: Int = 0,
    val selectedModuleItems: List<ModuleItem> = emptyList(),
)

data class ModuleItem(
    val id: Long?,
    val name: String?,
    val description: String?,
    val content: String,
)