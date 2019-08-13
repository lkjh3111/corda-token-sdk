package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.states.HouseState
import net.corda.core.contracts.Command
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.CollectSignaturesFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

abstract class Test: FlowLogic<SignedTransaction>() {

    fun stringToParty(user2: String): Party {
        return serviceHub.identityService.partiesFromName(user2, false).singleOrNull()
                ?: throw IllegalArgumentException("No match found for $user2")

    }

    fun inputStateRef(id: UniqueIdentifier): StateAndRef<HouseState> {
        val criteria = QueryCriteria.LinearStateQueryCriteria(linearId = listOf(id))
        return serviceHub.vaultService.queryBy<HouseState>(criteria = criteria).states.single()
    }

    fun verifyAndSign(tx: TransactionBuilder): SignedTransaction {
        tx.verify(serviceHub)
        return serviceHub.signInitialTransaction(tx)
    }

//    fun transactions(spiedOnMessage: ContractState): TransactionBuilder {
//        val notary = serviceHub.networkMapCache.notaryIdentities.first()
//        val txCommand = Command(WalletContract2.Commands.Register(),ourIdentity.owningKey)
//        return TransactionBuilder(notary)
//                .addOutputState(spiedOnMessage, WalletContract2.Wallet_ID)
//                .addCommand(txCommand)
//    }

    @Suspendable
    fun collectSignatures(partySignedTx: SignedTransaction):
            SignedTransaction = subFlow(CollectSignaturesFlow(partySignedTx, emptyList()))

    @Suspendable
    fun recordTransactions(partySignedTx: SignedTransaction):
            SignedTransaction = subFlow(FinalityFlow(partySignedTx, listOf()))

}