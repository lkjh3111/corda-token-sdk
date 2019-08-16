package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.AttachmentContract
import com.template.AttachmentContract.Companion.Attachment_ID
import com.template.states.AttachmentState
import net.corda.core.contracts.Command
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class AttachmentFlow(private val recipient: String,
                     private val attachId: String) : Test() {

    object SIGNING : ProgressTracker.Step("Signing transaction")

    override val progressTracker: ProgressTracker = ProgressTracker(SIGNING)

    @Suspendable
    override fun call(): SignedTransaction {
        // Create a trivial transaction with an output that describes the attachment, and the attachment itself
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val state = AttachmentState(SecureHash.parse(attachId), ourIdentity, stringToParty(recipient))
        val txCommand = Command(AttachmentContract.Commands.Upload(), ourIdentity.owningKey)
        val ptx = TransactionBuilder(notary)
                .addOutputState(state, Attachment_ID)
                .addCommand(txCommand)
                .addAttachment(SecureHash.parse(attachId))
        ptx.verify(serviceHub)
        progressTracker.currentStep = SIGNING
        val stx = serviceHub.signInitialTransaction(ptx)
        val session = initiateFlow(stringToParty(recipient))
        // Send the transaction to the other recipient
        return subFlow(FinalityFlow(stx,session))
    }
}

@InitiatedBy(AttachmentFlow::class)
class StoreAttachmentFlow(private val otherSide: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // As a non-participant to the transaction we need to record all states
        subFlow(ReceiveFinalityFlow(otherSide, statesToRecord = StatesToRecord.ALL_VISIBLE))
    }
}