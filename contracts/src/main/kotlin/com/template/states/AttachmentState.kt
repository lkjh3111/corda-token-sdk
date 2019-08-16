package com.template.states

import com.template.AttachmentContract
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party

@BelongsToContract(AttachmentContract::class)
data class AttachmentState(val attachmentHash: SecureHash.SHA256,
                           val sender: Party,
                           val recipient: Party,
                           override val linearId: UniqueIdentifier = UniqueIdentifier(),
                           override val participants: List<Party> = listOf(sender,recipient)
): LinearState