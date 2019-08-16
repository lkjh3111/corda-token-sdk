package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.google.common.collect.ImmutableList
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.rpc.*
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import com.template.states.HouseState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import org.bouncycastle.cms.bc.BcKEKEnvelopedRecipient
import java.util.*

@StartableByRPC
    class CreateNonFungibleHouseTokenFlow(private val address: String,
                                          private val valuation: Long,
                                          private val currency: String) : Test() {
        override val progressTracker = ProgressTracker()
        @Suspendable
        override fun call(): SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val houseToken = HouseState(address, valuation,currency,listOf(ourIdentity), 0, linearId = UniqueIdentifier())
            val transactionState = TransactionState(houseToken, notary = notary)
            return subFlow(CreateEvolvableTokens(transactionState))
        }
    }

    @StartableByRPC
    class IssueNonFungibleHouseTokenFlow(private val holder: String,
                                         private val tokenId: UniqueIdentifier) : Test() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(tokenId))
        val (state) = serviceHub.vaultService.queryBy(HouseState::class.java, queryCriteria).states[0]
        val houseToken = state.data
        val tokenPointer = houseToken.toPointer(houseToken.javaClass)
        val issuedTokenType = IssuedTokenType(ourIdentity, tokenPointer)
        val nonFungibleToken = NonFungibleToken(issuedTokenType, stringToParty(holder), UniqueIdentifier(), tokenPointer.getAttachmentIdForGenericParam())
        return subFlow(IssueTokens(ImmutableList.of(nonFungibleToken)))
        }
    }
@StartableByRPC
class IssueTokenFlow(private val amount: Long,
                     private val currency: String,
                     private val recipient: String) : Test() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        val token = FiatCurrency.getInstance(currency)
        return subFlow(IssueTokens(listOf(amount of token issuedBy ourIdentity heldBy stringToParty(recipient) )))
    }
}

    @StartableByRPC
    class MoveNonFungibleHouseTokenFlow(private val holder: String, private val tokenId: UniqueIdentifier) : Test() {

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(tokenId))
            val (state) = serviceHub.vaultService.queryBy(HouseState::class.java, queryCriteria).states[0]
            val houseToken = state.data
            val tokenPointer = houseToken.toPointer(houseToken.javaClass)
            val partyAndToken = PartyAndToken(stringToParty(holder), tokenPointer)
            return subFlow<Any>(MoveNonFungibleTokens(partyAndToken)) as SignedTransaction
        }
    }


    @StartableByRPC
    class RedeemNonFungibleHouseTokenFlow(private val issuer: String, private val tokenId: UniqueIdentifier ) : Test() {

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(tokenId))
            val (state) = serviceHub.vaultService.queryBy(HouseState::class.java, queryCriteria).states[0]
            val houseState = state.data
            val token = houseState.toPointer(houseState.javaClass)
            return subFlow(RedeemNonFungibleTokens(token, stringToParty(issuer)))
        }
    }


    @StartableByRPC
    class UpdateNonFungibleHouseTokenFlow(private val new_valuation: Long,
                                          private val tokenId: UniqueIdentifier) : Test() {

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(tokenId))
            val oldHouseState = serviceHub.vaultService.queryBy(HouseState::class.java, queryCriteria).states[0]
            val newHouseState = oldHouseState.state.data.copy(valuation = new_valuation)
            return subFlow(UpdateEvolvableToken(oldStateAndRef = oldHouseState, newState = newHouseState))
        }
    }
