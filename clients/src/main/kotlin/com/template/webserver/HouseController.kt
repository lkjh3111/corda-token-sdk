package com.template.webserver

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.template.flows.*
import com.template.models.*
import com.template.states.HouseState
import net.corda.core.messaging.vaultQueryBy
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private const val CONTROLLER_NAME = "config.controller.name"
//@Value("\${$CONTROLLER_NAME}") private val controllerName: String
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class HouseController(
        private val rpc: NodeRPCConnection
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy

    @GetMapping(value = "/house-info", produces = arrayOf("application/json"))
    private fun getHouseStates(): ResponseEntity<Map<String, Any>> {
        val (status, result) = try {
            val stateRef = rpc.proxy.vaultQueryBy<HouseState>().states
            val states = stateRef.map { it.state.data }
            val list = states.map {
                HouseModel(
                        address = it.address,
                        valuation = it.valuation,
                        currency = it.currency,
                        maintainers = it.maintainers.toString(),
                        houseId = it.linearId)
            }
            HttpStatus.CREATED to list
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful in getting House Information"
        } else {
            "message" to "Failed to get House Information"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))


    }

    @GetMapping(value = "/house-token-info", produces = arrayOf("application/json"))
    private fun getHouseToken(): ResponseEntity<Map<String, Any>> {
        val (status, result) = try {
            val stateRef = rpc.proxy.vaultQueryBy<NonFungibleToken>().states
            val states = stateRef.map { it.state.data }
            val list = states.map {
                NonFungibleModel(
                        token = it.token.toString(),
                        holder = it.holder.toString()
                        )
            }
            HttpStatus.CREATED to list
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful in getting House Token Information"
        } else {
            "message" to "Failed to get House Token Information"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))


    }

    @GetMapping(value = "/money-info", produces = arrayOf("application/json"))
    private fun getMoneyInfo(): ResponseEntity<Map<String, Any>> {
        val (status, result) = try {
            val stateRef = rpc.proxy.vaultQueryBy<FungibleToken>().states
            val states = stateRef.map { it.state.data }
            val list = states.map {
                FungibleModel(
                        amount = it.amount.toString(),
                        holder = it.holder.toString()
                )
            }
            HttpStatus.CREATED to list
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful in getting Money Information"
        } else {
            "message" to "Failed to get House Money Information"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))


    }

    @PostMapping(value = "/create-house-token", produces = arrayOf("application/json"))
    private fun createHouse(@RequestBody createHouse: CreateHouse): ResponseEntity<Map<String, Any>> {

        val (status, result) = try {
            val create = CreateHouse(
                    address = createHouse.address,
                    valuation = createHouse.valuation,
                    currency = createHouse.currency

            )

            proxy.startFlowDynamic(
                    CreateNonFungibleHouseTokenFlow::class.java,
                    create.address,
                    create.valuation,
                    create.currency
            )

            HttpStatus.CREATED to "Success"
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status.value()
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful in creating Non-Fungible House Token"
        } else {
            "message" to "Failed to create Non-Fungible House Token"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))

    }


    @PostMapping(value = "/issue-house-token", produces = arrayOf("application/json"))
    private fun issueHouse(@RequestBody issueHouse: IssueHouse): ResponseEntity<Map<String, Any>> {

        val (status, result) = try {
            val issue = IssueHouse(
                    recipient = issueHouse.recipient,
                    houseId = issueHouse.houseId

            )
            proxy.startFlowDynamic(
                    IssueNonFungibleHouseTokenFlow::class.java,
                    issue.recipient,
                    issue.houseId
            )

            HttpStatus.CREATED to "Success"
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status.value()
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful in issuing House Token"
        } else {
            "message" to "Failed to issue House Token"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))

    }

    @PostMapping(value = "/issue-money", produces = arrayOf("application/json"))
    private fun issueToken(@RequestBody issueToken: IssueToken): ResponseEntity<Map<String, Any>> {

        val (status, result) = try {
            val issue = IssueToken(
                    amount = issueToken.amount,
                    currency = issueToken.currency,
                    recipient = issueToken.recipient

            )
            proxy.startFlowDynamic(
                    IssueTokenFlow::class.java,
                    issue.amount,
                    issue.currency,
                    issue.recipient
            )

            HttpStatus.CREATED to "Success"
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status.value()
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful in issuing Money"
        } else {
            "message" to "Failed to issue Money"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))

    }


    @PostMapping(value = "/sell-house", produces = arrayOf("application/json"))
    private fun sellhouse(@RequestBody sellhouse: SellHouse): ResponseEntity<Map<String, Any>> {

        val (status, result) = try {
            val sell = SellHouse(
                    buyer = sellhouse.buyer,
                    houseId = sellhouse.houseId
            )
            proxy.startFlowDynamic(
                    SellHouseFlow::class.java,
                    sell.buyer,
                    sell.houseId
            )

            HttpStatus.CREATED to "Success"
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status.value()
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful in creating ContractState of type UserState"
        } else {
            "message" to "Failed to create ContractState of type UserState"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))

    }


    @PostMapping(value = "/move-house", produces = arrayOf("application/json"))
    private fun moveHouse(@RequestBody moveHouse: MoveHouse): ResponseEntity<Map<String, Any>> {

        val (status, result) = try {
            val move = MoveHouse(
                    new_holder = moveHouse.new_holder,
                    houseId = moveHouse.houseId

            )
            proxy.startFlowDynamic(
                    MoveNonFungibleHouseTokenFlow::class.java,
                    move.new_holder,
                    move.houseId
            )

            HttpStatus.CREATED to "Success"
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status.value()
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful in moving House Token"
        } else {
            "message" to "Failed to move House Token"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))

    }


    @PostMapping(value = "/redeem-house", produces = arrayOf("application/json"))
    private fun redeemHouse(@RequestBody redeemHouse: RedeemHouse): ResponseEntity<Map<String, Any>> {

        val (status, result) = try {
            val redeem = RedeemHouse(
                    issuer = redeemHouse.issuer,
                    houseId = redeemHouse.houseId

            )
            proxy.startFlowDynamic(
                    RedeemNonFungibleHouseTokenFlow::class.java,
                    redeem.issuer,
                    redeem.houseId
            )

            HttpStatus.CREATED to "Success"
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status.value()
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful in redeeming House Token"
        } else {
            "message" to "Failed to redeem House Token"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))

    }
    @PostMapping(value = "/update-house-valuation", produces = arrayOf("application/json"))
    private fun updateHouse(@RequestBody updateHouse: UpdateHouseValuation): ResponseEntity<Map<String, Any>> {

        val (status, result) = try {
            val update = UpdateHouseValuation(
                    new_valuation = updateHouse.new_valuation,
                    houseId = updateHouse.houseId

            )
            proxy.startFlowDynamic(
                    UpdateNonFungibleHouseTokenFlow::class.java,
                    update.new_valuation,
                    update.houseId
            )

            HttpStatus.CREATED to "Success"
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status.value()
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful in redeeming House Token"
        } else {
            "message" to "Failed to redeem House Token"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))

    }

}