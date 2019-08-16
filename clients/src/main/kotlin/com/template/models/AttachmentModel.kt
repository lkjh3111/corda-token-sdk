package com.template.models

import com.fasterxml.jackson.annotation.JsonCreator
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash

data class AttachmentModel(
        val attachId: String,
        val sender: String,
        val recipient: String,
        val linearId: UniqueIdentifier
)

data class SendFile @JsonCreator constructor(
        val recipient: String,
        val attachId: String
)