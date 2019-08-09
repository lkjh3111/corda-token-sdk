package com.template.states

import com.google.common.collect.ImmutableList
import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.template.RealEstateEvolvableTokenTypeContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

import java.math.BigDecimal
import java.util.Objects

@BelongsToContract(RealEstateEvolvableTokenTypeContract::class)
class RealEstateEvolvableTokenType(val valuation: BigDecimal, val maintainer: Party,
                                   override val linearId: UniqueIdentifier, override val fractionDigits: Int) : EvolvableTokenType() {

    override val maintainers: List<Party>
        get() = ImmutableList.of(maintainer)

    fun getUniqueIdentifier(): UniqueIdentifier {
        return linearId
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as RealEstateEvolvableTokenType?
        return fractionDigits == that!!.fractionDigits &&
                valuation == that.valuation &&
                maintainer == that.maintainer &&
                linearId == that.linearId
    }

    override fun hashCode(): Int {
        return Objects.hash(valuation, maintainer, linearId, fractionDigits)
    }
}
