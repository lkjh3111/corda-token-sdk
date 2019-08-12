package com.template

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import com.template.states.HouseState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction


class HouseContract : EvolvableTokenContract(), Contract {

    override fun additionalCreateChecks(tx: LedgerTransaction) {
        // Not much to do for this example token.
        val newHouse = tx.outputStates.single() as HouseState
        //val valuation = newHouse.valuation
        newHouse.apply {
            require(valuation > 0) { "Valuation must be greater than zero." }
        }
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        val oldHouse = tx.inputStates.single() as HouseState
        val newHouse = tx.outputStates.single() as HouseState
        require(oldHouse.address == newHouse.address) { "The address cannot change." }
        require(newHouse.valuation > 0) { "Valuation must be greater than zero." }
    }
}