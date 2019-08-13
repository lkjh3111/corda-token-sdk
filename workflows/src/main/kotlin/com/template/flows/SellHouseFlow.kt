package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import com.r3.corda.lib.tokens.workflows.internal.selection.TokenSelection
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
import com.r3.corda.lib.tokens.workflows.utilities.ourSigningKeys
import com.template.states.HouseState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@StartableByRPC
@InitiatingFlow
class SellHouseFlow(val buyer: String, val linearId: UniqueIdentifier) : Test() {
    @Suspendable
    override fun call(): SignedTransaction {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(linearId))
        val tokenStateAndRef = serviceHub.vaultService.queryBy<HouseState>(queryCriteria).states.single()
        val house = tokenStateAndRef.state.data
        val housePtr = house.toPointer<HouseState>()
        val txBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
        val token = FiatCurrency.getInstance(house.currency)
        addMoveNonFungibleTokens(txBuilder, serviceHub, housePtr, stringToParty(buyer))
        val session = initiateFlow(stringToParty(buyer))
        session.send(house.valuation of token)
        val inputs = subFlow(ReceiveStateAndRefFlow<FungibleToken>(session))
        val outputs = session.receive<List<FungibleToken>>().unwrap { it }
        addMoveTokens(txBuilder, inputs, outputs)
        val initialStx = serviceHub.signInitialTransaction(txBuilder, ourIdentity.owningKey)
        val stx = subFlow(CollectSignaturesFlow(initialStx, listOf(session)))
        subFlow(UpdateDistributionListFlow(stx))
        return subFlow(FinalityFlow(stx, listOf(session)))
    }
}

    @InitiatedBy(SellHouseFlow::class)
    class SellHouseFlowHandler(val otherSession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val priceNotification = otherSession.receive<Amount<TokenType>>().unwrap { it }
            val (inputs, outputs) = TokenSelection(serviceHub).generateMove(
                    lockId = runId.uuid,
                    partyAndAmounts = listOf(PartyAndAmount(otherSession.counterparty, priceNotification)),
                    changeHolder = ourIdentity
            )
            subFlow(SendStateAndRefFlow(otherSession, inputs))
            otherSession.send(outputs)
            subFlow(object : SignTransactionFlow(otherSession) {
                override fun checkTransaction(stx: SignedTransaction) {

                }
            })
            subFlow(ReceiveFinalityFlow(otherSession))
        }
    }




