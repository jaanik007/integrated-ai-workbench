package com.jaanik.integratedaiworkbench.backend.context

/** Future implementations select only allowed, text-based project context. */
interface ContextService {
    fun collect(request: GroundingRequest): GroundingContext
}
