package com.creditrepairai.v2.auth

/** Invalidate completion tickets on deletion, demo replacement and every account change. */
class SessionFence(private var owner: String?) {
    data class Ticket(val owner: String?, val generation: Long)
    private var generation = 0L
    @Synchronized fun ticket() = Ticket(owner, generation)
    @Synchronized fun invalidate(newOwner: String? = owner) { owner = newOwner; generation++ }
    @Synchronized fun accepts(ticket: Ticket, currentOwner: String?) =
        ticket.owner != null && ticket.owner == owner && owner == currentOwner && ticket.generation == generation
}
