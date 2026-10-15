package com.jaanik.integratedaiworkbench.frontend.tabs

/** A visible Workbench tab that can reload saved application state without owning it. */
interface RefreshableWorkbenchTab {
    fun refreshFromServices()
}
