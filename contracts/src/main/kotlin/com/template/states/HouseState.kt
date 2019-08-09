package com.template.states


import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.template.HouseContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.util.*

@BelongsToContract(HouseContract::class)
data class HouseState(
        val address: String,
        val valuation: Amount<Currency>,
        override val maintainers: List<Party>,
        override val fractionDigits: Int = 0,
        override val linearId: UniqueIdentifier
) : EvolvableTokenType()