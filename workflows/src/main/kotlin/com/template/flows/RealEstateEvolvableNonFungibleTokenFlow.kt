package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.google.common.collect.ImmutableList
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.utilities.getAttachmentIdForGenericParam
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.RedeemNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import com.template.states.RealEstateEvolvableTokenType
import net.corda.core.contracts.*
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction

import java.math.BigDecimal
import java.util.UUID

object RealEstateEvolvableNonFungibleTokenFlow {

    /**
     * Create NonFungible Token in ledger
     */
    @StartableByRPC
    class CreateEvolvableTokenFlow(private val valuation: BigDecimal) : FlowLogic<SignedTransaction>() {

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            //grab the notary
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            //create token type
            val evolvableTokenType = RealEstateEvolvableTokenType(valuation, ourIdentity,
                    UniqueIdentifier(), 0)

            //warp it with transaction state specifying the notary
            val transactionState = TransactionState(evolvableTokenType, notary=notary)

            //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
            return subFlow(CreateEvolvableTokens(transactionState))
        }
    }

    /**
     * Issue Non Fungible Token
     */
    @StartableByRPC
    class IssueEvolvableTokenFlow(private val tokenId: String, private val holder: Party) : FlowLogic<SignedTransaction>() {

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            //using id of my house to grab the house from db.
            // you can use any custom criteria depending on your requirements
            val uuid = UUID.fromString(tokenId)

            //construct the query criteria
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(uuid), null,
                    Vault.StateStatus.UNCONSUMED, null)

            // grab the house off the ledger
            val (state) = serviceHub.vaultService.queryBy(RealEstateEvolvableTokenType::class.java, queryCriteria).states[0]
            val evolvableTokenType = state.data

            //get the pointer pointer to the house
            val tokenPointer = evolvableTokenType.toPointer(evolvableTokenType.javaClass)

            //assign the issuer to the house type who will be issuing the tokens
            val issuedTokenType = IssuedTokenType(ourIdentity, tokenPointer)

            //mention the current holder also
            val nonFungibleToken = NonFungibleToken(issuedTokenType, holder, UniqueIdentifier(), tokenPointer.getAttachmentIdForGenericParam())

            //call built in flow to issue non fungible tokens
            return subFlow(IssueTokens(ImmutableList.of(nonFungibleToken)))
        }
    }

    /**
     * Move created non fungible token to other party
     */
    @StartableByRPC
    class MoveEvolvableTokenFlow(private val tokenId: String, private val holder: Party) : FlowLogic<SignedTransaction>() {

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            //using id of my house to grab the house from db.
            //you can use any custom criteria depending on your requirements
            val uuid = UUID.fromString(tokenId)

            //construct the query criteria
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(uuid), null,
                    Vault.StateStatus.UNCONSUMED, null)

            // grab the house off the ledger
            val (state) = serviceHub.vaultService.queryBy(RealEstateEvolvableTokenType::class.java, queryCriteria).states[0]
            val evolvableTokenType = state.data

            //get the pointer pointer to the house
            val tokenPointer = evolvableTokenType.toPointer(evolvableTokenType.javaClass)

            //specify the party who will be the new owner of the token
            val partyAndToken = PartyAndToken(holder, tokenPointer)
            return subFlow<Any>(MoveNonFungibleTokens(partyAndToken)) as SignedTransaction
        }
    }

    /**
     * Holder Redeems non fungible token issued by issuer
     */
    @StartableByRPC
    class RedeemHouseToken(private val tokenId: String, private val issuer: Party) : FlowLogic<SignedTransaction>() {

        @Suspendable
        @Throws(FlowException::class)
        override fun call(): SignedTransaction {
            //using id of my house to grab the house from db.
            //you can use any custom criteria depending on your requirements
            val uuid = UUID.fromString(tokenId)

            //construct the query criteria
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(uuid), null,
                    Vault.StateStatus.UNCONSUMED, null)

            // grab the house off the ledger
            val (state) = serviceHub.vaultService.queryBy(RealEstateEvolvableTokenType::class.java, queryCriteria).states[0]
            val evolvableTokenType = state.data

            //get the pointer pointer to the house
            val token = evolvableTokenType.toPointer(evolvableTokenType.javaClass)

            //call built in flow to redeem the tokens
            return subFlow(RedeemNonFungibleTokens(token, issuer))
        }
    }
}//Instantiation not allowed

