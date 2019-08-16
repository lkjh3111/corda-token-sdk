package com.template.models

import com.fasterxml.jackson.annotation.JsonCreator
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party

data class HouseModel(
        val address: String,
        val valuation: Long,
        val currency: String,
        val maintainers: String,
        val houseId: UniqueIdentifier
)
data class NonFungibleModel(
        val token: String,
        val holder: String
)
data class FungibleModel(
        val amount: String,
        val holder: String
)

data class CreateHouse @JsonCreator constructor(
        val address: String,
        val valuation: Long,
        val currency: String
)

data class IssueHouse @JsonCreator constructor(
        val recipient: String,
        val houseId: UniqueIdentifier
)

data class IssueToken @JsonCreator constructor(
        val amount:Long,
        val currency: String,
        val recipient: String
)

data class SellHouse @JsonCreator constructor(
        val buyer: String,
        val houseId: UniqueIdentifier
)

data class MoveHouse @JsonCreator constructor(

        val new_holder: String,
        val houseId: UniqueIdentifier
)

data class RedeemHouse @JsonCreator constructor(
        val issuer: String,
        val houseId: UniqueIdentifier
)


data class UpdateHouseValuation @JsonCreator constructor(
        val new_valuation: Long,
        val houseId: UniqueIdentifier
)