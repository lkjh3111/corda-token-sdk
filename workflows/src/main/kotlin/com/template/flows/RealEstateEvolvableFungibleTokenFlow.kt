package com.template.flows


import co.paralleluniverse.fibers.Suspendable
import com.google.common.collect.ImmutableList
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndAmount
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
import java.math.BigDecimal
import java.util.*

/**
 * Create,Issue,Move,Redeem token flows for a house asset on ledger
 */
object RealEstateEvolvableFungibleTokenFlow {

    /**
     * Create Fungible Token for a house asset on ledger
     */
    @StartableByRPC
    class CreateEvolvableFungibleTokenFlow(private val valuation: BigDecimal) : FlowLogic<SignedTransaction?>() {

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            val evolvableTokenType = RealEstateEvolvableTokenType(valuation, ourIdentity,
                    UniqueIdentifier(), 0)
            val transactionState = TransactionState(evolvableTokenType, notary= notary)

            return subFlow(CreateEvolvableTokens(transactionState))

        }
    }

    /**
     * Issue Fungible Token against an evolvable house asset on ledger
     */
    @StartableByRPC
    class IssueEvolvableFungibleTokenFlow(private val tokenId: String, private val quantity: Int, private val holder: Party) : FlowLogic<SignedTransaction>() {

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            //get uuid from input tokenId
            val uuid = UUID.fromString(tokenId)

            //create criteria to get all unconsumed house states on ledger with uuid as input tokenId
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(uuid), null,
                    Vault.StateStatus.UNCONSUMED)
            val (state) = serviceHub.vaultService.queryBy(RealEstateEvolvableTokenType::class.java, queryCriteria).states[0]

            //get the RealEstateEvolvableTokenType object
            val evolvableTokenType = state.data

            //get the pointer pointer to the house
            val tokenPointer = evolvableTokenType.toPointer(evolvableTokenType.javaClass)

            //assign the issuer to the house type who will be issuing the tokens
            val issuedTokenType = IssuedTokenType(ourIdentity, tokenPointer)

            //specify how much amount to issue to holder
            val amount = Amount(quantity.toLong(), issuedTokenType)

            //create fungible amount specifying the new owner
            val fungibleToken = FungibleToken(amount, holder, tokenPointer.getAttachmentIdForGenericParam())

            //use built in flow for issuing tokens on ledger
            return subFlow(IssueTokens(ImmutableList.of(fungibleToken)))
            //return SignedTransaction()

        }
    }

    /**
     * Move created fungible tokens to other party
     */
    @StartableByRPC
    class MoveEvolvableFungibleTokenFlow(private val tokenId: String, private val holder: Party, private val quantity: Int) : FlowLogic<SignedTransaction>() {

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            //get uuid from input tokenId
            val uuid = UUID.fromString(tokenId)

            //create criteria to get all unconsumed house states on ledger with uuid as input tokenId
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(uuid), null,
                    Vault.StateStatus.UNCONSUMED, null)
            val (state) = serviceHub.vaultService.queryBy(RealEstateEvolvableTokenType::class.java, queryCriteria).states[0]

            //get the RealEstateEvolvableTokenType object
            val evolvableTokenType = state.data

            //get the pointer pointer to the house
            val tokenPointer = evolvableTokenType.toPointer(evolvableTokenType.javaClass)

            //specify how much amount to transfer to which holder
            val amount = Amount(quantity.toLong(), tokenPointer)
            val partyAndAmount = PartyAndAmount(holder, amount)

            //use built in flow to move fungible tokens to holder
            val move = MoveFungibleTokens(amount = Amount(quantity.toLong(), tokenPointer),holder = holder)
            return subFlow(move)
            //return null
        }
    }

    /**
     * Holder Redeems fungible token issued by issuer
     */
    @StartableByRPC
    class RedeemHouseFungibleTokenFlow(private val tokenId: String, private val issuer: Party, private val quantity: Int) : FlowLogic<SignedTransaction>() {

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            //get uuid from input tokenId
            val uuid = UUID.fromString(tokenId)

            //create criteria to get all unconsumed house states on ledger with uuid as input tokenId
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(uuid), null,
                    Vault.StateStatus.UNCONSUMED, null)
            val (state) = serviceHub.vaultService.queryBy(RealEstateEvolvableTokenType::class.java, queryCriteria).states[0]

            //get the RealEstateEvolvableTokenType object
            val evolvableTokenType = state.data

            //get the pointer pointer to the house
            val tokenPointer = evolvableTokenType.toPointer(evolvableTokenType.javaClass)

            //specify how much amount quantity of tokens of type token parameter
            val amount = Amount(quantity.toLong(), tokenPointer)

            //call built in redeem flow to redeem tokens with issuer
            return subFlow(RedeemFungibleTokens(amount = Amount(quantity.toLong(), tokenPointer), issuer = issuer))
            //return null
        }
    }
}//Instantiation not allowed

