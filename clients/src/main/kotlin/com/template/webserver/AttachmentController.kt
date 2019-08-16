package com.template.webserver

import com.template.flows.AttachmentFlow
import com.template.flows.CreateNonFungibleHouseTokenFlow
import com.template.models.AttachmentModel
import com.template.models.CreateHouse
import com.template.models.HouseModel
import com.template.models.SendFile
import com.template.states.AttachmentState
import com.template.states.HouseState
import net.corda.client.rpc.CordaRPCClient
import net.corda.core.crypto.SecureHash
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.AttachmentId
import net.corda.core.node.services.vault.AttachmentQueryCriteria
import net.corda.core.node.services.vault.Builder
import net.corda.core.utilities.NetworkHostAndPort
import org.bouncycastle.cms.RecipientId.password
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.annotation.Resource
import java.io.File


private const val CONTROLLER_NAME = "config.controller.name"
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class AttachmentController(private val rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy = rpc.proxy


    @GetMapping(value = "/files", produces = arrayOf("application/json"))
    private fun getHouseStates(): ResponseEntity<Map<String, Any>> {
        val (status, result) = try {
            val stateRef = rpc.proxy.vaultQueryBy<AttachmentState>().states
            val states = stateRef.map { it.state.data }
            val list = states.map {
                AttachmentModel(
                        attachId = it.attachmentHash.toString(),
                        sender = it.sender.toString(),
                        recipient = it.recipient.toString(),
                        linearId = it.linearId)
            }
            HttpStatus.CREATED to list
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful in getting Attachment State"
        } else {
            "message" to "Failed to get Attachment State"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }

    @PostMapping(value = "/send-file", produces = arrayOf("application/json"))
    private fun sendFile(@RequestBody sendFile: SendFile): ResponseEntity<Map<String, Any>> {

        val (status, result) = try {
            val send = SendFile(
                    recipient = sendFile.recipient,
                    attachId = sendFile.attachId
            )

            proxy.startFlowDynamic(
                    AttachmentFlow::class.java,
                    send.recipient,
                    send.attachId
            )

            HttpStatus.CREATED to "Success"
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to "No data"
        }
        val stat = "status" to status.value()
        val mess = if (status == HttpStatus.CREATED) {
            "message" to "Successful in sending a file"
        } else {
            "message" to "Failed to send a file"
        }
        val res = "result" to result
        return ResponseEntity.status(status).body(mapOf(stat, mess, res))
    }

    @PostMapping("/attachments")
    fun upload(@RequestParam file: MultipartFile ): ResponseEntity<String> {
    //fun upload(@RequestParam file: MultipartFile ): ResponseEntity<String> {
        val filename = file.originalFilename
        var files = File(filename)
        logger.info(filename)
        val hash: SecureHash = if (!(files.extension == "zip" || files.extension == "jar")) {
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss")
            val formatted = current.format(formatter)
            val zipName = "$formatted.zip"
            logger.info(zipName)
            FileOutputStream(zipName).use { fileOutputStream ->
                ZipOutputStream(fileOutputStream).use { zipOutputStream ->
                    val zipEntry = ZipEntry(filename)
                    zipOutputStream.putNextEntry(zipEntry)
                    file.inputStream.copyTo(zipOutputStream, 1024)
                }
            }
            proxy.uploadAttachment(FileInputStream(zipName))

        } else {
            proxy.uploadAttachment(
                    jar = file.inputStream
            )
        }
        //val hash: SecureHash = proxy.uploadAttachment(jar = file.inputStream)

        return ResponseEntity.created(URI.create("attachments/$hash")).body("Attachment uploaded with hash - $hash")
    }



    @GetMapping("/{hash}")
    fun downloadByHash(@PathVariable hash: String): ResponseEntity<InputStreamResource> {
        val inputStream = InputStreamResource(proxy.openAttachment(SecureHash.parse(hash)))
        return ResponseEntity.ok().header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"$hash.zip\""
        ).body(inputStream)
    }

}