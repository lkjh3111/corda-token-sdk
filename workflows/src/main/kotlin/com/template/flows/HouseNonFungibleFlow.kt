package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.google.common.collect.ImmutableList
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam
import com.r3.corda.lib.tokens.money.FiatCurrency
import com.r3.corda.lib.tokens.workflows.flows.rpc.*
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import com.template.states.ExampleEvolvableTokenType
import com.template.states.HouseState
import com.template.states.RealEstateEvolvableTokenType
import net.corda.core.contracts.Amount
import net.corda.core.contracts.TransactionState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import java.util.*

//object HouseNonFungibleFlow {
    @StartableByRPC
    class CreateNonFungibleHouseTokenFlow(private val address: String,
                                          private val valuation: Amount<Currency>) : FlowLogic<SignedTransaction>() {
        override val progressTracker = ProgressTracker()
        @Suspendable
        override fun call(): SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val houseToken = HouseState(address,valuation, listOf(ourIdentity),0, linearId = UniqueIdentifier())
            val transactionState = TransactionState(houseToken, notary = notary)
            return subFlow(CreateEvolvableTokens(transactionState))
        }
    }

    @StartableByRPC
    class IssueNonFungibleHouseTokenFlow(private val tokenId: UniqueIdentifier, private val holder: Party) : FlowLogic<SignedTransaction>() {

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(tokenId))
            val (state) = serviceHub.vaultService.queryBy(HouseState::class.java, queryCriteria).states[0]
            val houseToken = state.data
            val tokenPointer = houseToken.toPointer(houseToken.javaClass)
            val issuedTokenType = IssuedTokenType(ourIdentity, tokenPointer)
            val nonFungibleToken = NonFungibleToken(issuedTokenType, holder, UniqueIdentifier(), tokenPointer.getAttachmentIdForGenericParam())
            return subFlow(IssueTokens(ImmutableList.of(nonFungibleToken)))
        }
    }




        @StartableByRPC
        class MoveNonFungibleHouseTokenFlow(private val tokenId: UniqueIdentifier, private val holder: Party) : FlowLogic<SignedTransaction>() {

            @Suspendable
            @Throws(FlowException::class)
            override fun call(): SignedTransaction {
                val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(tokenId))
                val (state) = serviceHub.vaultService.queryBy(HouseState::class.java, queryCriteria).states[0]
                val houseToken = state.data
                val tokenPointer = houseToken.toPointer(houseToken.javaClass)
                val partyAndToken = PartyAndToken(holder, tokenPointer)
                return subFlow<Any>(MoveNonFungibleTokens(partyAndToken)) as SignedTransaction
            }
        }


        @StartableByRPC
        class RedeemNonFungibleHouseTokenFlow(private val tokenId: UniqueIdentifier, private val issuer: Party) : FlowLogic<SignedTransaction>() {

            @Suspendable
            @Throws(FlowException::class)
            override fun call(): SignedTransaction {
                val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(tokenId))
                val (state) = serviceHub.vaultService.queryBy(HouseState::class.java, queryCriteria).states[0]
                val houseState = state.data
                val token = houseState.toPointer(houseState.javaClass)
                return subFlow(RedeemNonFungibleTokens(token, issuer))
            }
        }

        @StartableByRPC
        class UpdateNonFungibleHouseTokenFlow(private val new_amount: Amount<Currency>,
                                              private val tokenId: UniqueIdentifier) : FlowLogic<SignedTransaction>() {

            @Suspendable
            @Throws(FlowException::class)
            override fun call(): SignedTransaction {
                val queryCriteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(tokenId))
                val oldHouseState = serviceHub.vaultService.queryBy(HouseState::class.java, queryCriteria).states[0]
                val newHouseState = oldHouseState.state.data.copy(valuation = new_amount)
                //val token = ouseState.toPointer(newHouseState.javaClass)
                //return subFlow(RedeemNonFungibleTokens(token, holder))
                return subFlow(UpdateEvolvableToken(oldStateAndRef = oldHouseState, newState = newHouseState))
            }
        }


//}