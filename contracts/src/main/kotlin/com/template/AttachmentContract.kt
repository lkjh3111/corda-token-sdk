package com.template

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction


class AttachmentContract : Contract {

    companion object{
        const val Attachment_ID = "com.template.AttachmentContract"
    }

    interface Commands : CommandData {
        class Upload : TypeOnlyCommandData(), Commands
        //class Download : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()

        when (command.value){
            is Commands.Upload -> requireThat {
            }

        }

    }
}