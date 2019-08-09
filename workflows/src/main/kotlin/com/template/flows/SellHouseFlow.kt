//package com.template.flows
//
//import co.paralleluniverse.fibers.Suspendable
//import com.r3.corda.lib.tokens.contracts.states.FungibleToken
//import com.r3.corda.lib.tokens.contracts.types.TokenType
//import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
//import com.r3.corda.lib.tokens.workflows.utilities.getPreferredNotary
//import com.template.states.HouseState
//import net.corda.core.contracts.Amount
//import net.corda.core.flows.FlowLogic
//import net.corda.core.flows.InitiatingFlow
//import net.corda.core.flows.ReceiveStateAndRefFlow
//import net.corda.core.flows.StartableByRPC
//import net.corda.core.identity.Party
//import net.corda.core.serialization.CordaSerializable
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.transactions.TransactionBuilder
//import net.corda.core.utilities.unwrap
//
//@StartableByRPC
//@InitiatingFlow
//class SellHouseFlow(val house: HouseState, val newHolder: Party) : FlowLogic<SignedTransaction>() {
//    @Suspendable
//    override fun call(): SignedTransaction {
//        //TODO("Implement delivery versus payment logic.")
//        val housePtr = house.toPointer<HouseState>()
//        // We can specify preferred notary in cordapp config file, otherwise the first one from network parameters is chosen.
//        val txBuilder = TransactionBuilder(notary = getPreferredNotary(serviceHub))
//        addMoveTokens(txBuilder,newHolder, )
//
//        val session = initiateFlow(newHolder)
//        // Ask for input stateAndRefs - send notification with the amount to exchange.
//        session.send(PriceNotification(house.valuation))
//        // Receive GBP states back.
//        val inputs = subFlow(ReceiveStateAndRefFlow<FungibleToken>(session))
//        // Receive outputs.
//        val outputs = session.receive<List<FungibleToken>>().unwrap { it }
//        addMoveTokens(txBuilder, inputs, outputs)
//        subFlow(IdentitySyncFlow.Send(session, txBuilder.toWireTransaction(serviceHub)))
//
//    }
//    @CordaSerializable
//    data class PriceNotification(val amount: Amount<TokenType>)
//}
//
//
//
