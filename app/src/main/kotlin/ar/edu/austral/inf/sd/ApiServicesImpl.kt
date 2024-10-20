package ar.edu.austral.inf.sd

import ar.edu.austral.inf.sd.server.api.*
import ar.edu.austral.inf.sd.server.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.MultipartBodyBuilder
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@Component
class ApiServicesImpl : RegisterNodeApiService, RelayApiService, PlayApiService, UnregisterNodeApiService,
    ReconfigureApiService {

    @Value("\${server.name:nada}")
    private val myServerName: String = ""
    @Value("\${server.host:localhost}")
    private val myServerHost: String = ""
    @Value("\${server.port:8080}")
    private val myServerPort: Int = 0
    @Value("\${register.host:localhost}")
    private val registerHost: String = ""
    @Value("\${register.port:8080}")
    private val registerPort: Int = 0
    private val nodes: MutableList<RegisterRequest> = mutableListOf()
    private var nextNode: RegisterResponse? = null
    private val messageDigest = MessageDigest.getInstance("SHA-512")
    private val salt = Base64.getEncoder().encodeToString(Random.nextBytes(9))
    private val currentRequest
        get() = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
    private var resultReady = CountDownLatch(1)
    private var currentMessageWaiting = MutableStateFlow<PlayResponse?>(null)
    private var currentMessageResponse = MutableStateFlow<PlayResponse?>(null)
    private var xGameTimestamp: Int = 0
    @Value("\${server.timeout:1000}")
    private val timeout: Int = 10000
    private var gameFailed: Int = 0
    private val myUUID = UUID.randomUUID()

    override fun registerNode(host: String?, port: Int?, uuid: UUID?, salt: String?, name: String?): RegisterResponse {

        val nextNode = if (nodes.isEmpty()) {
            // es el primer nodo
            val me = RegisterRequest(currentRequest.serverName, myServerPort, name!!, null, null)
            nodes.add(me)
            me
        } else {
            nodes.last()
        }
        val node = RegisterRequest(host!!, port!!, name!!, myUUID, this.salt)
        if (nodes.contains(node)) throw AcceptedException(RegisterResponse(nextNode.host, nextNode.port, timeout, ++xGameTimestamp, node.uuid), "Registración existente pero válida. Listo para jugar.")
        if (nodes.any { it.uuid == node.uuid && it.salt != node.salt }) throw UnauthorizedException("Estás tratando de estafarme... El UUID ya existe pero mandaste la clave incorrecta.")
        nodes.add(node)

        return RegisterResponse(nextNode.host, nextNode.port, timeout, ++xGameTimestamp, node.uuid)
    }

    override fun relayMessage(message: String, signatures: Signatures, xGameTimestamp: Int?): Signature {
        val receivedHash = doHash(message.encodeToByteArray(), salt)
        val receivedContentType = currentRequest.getPart("message")?.contentType ?: "nada"
        val receivedLength = message.length
        if (nextNode != null) {
            // Soy un relé. busco el siguiente y lo mando
            if (xGameTimestamp != null) {
                if (xGameTimestamp < this.xGameTimestamp) throw BadRequestException("Tu reloj atrasa...")
            }

            val currentSignature = clientSign(message, receivedContentType)
            val updatedSignatures = signatures.items.toMutableList()
            updatedSignatures.add(currentSignature)

            val newSignatures = Signatures(updatedSignatures)

            sendRelayMessage(message, receivedContentType, nextNode!!, newSignatures)
        } else {
            // me llego algo, no lo tengo que pasar
            if (currentMessageWaiting.value == null) throw BadRequestException("no waiting message")
            val current = currentMessageWaiting.getAndUpdate { null }!!
            val response = current.copy(
                contentResult = if (receivedHash == current.originalHash) "Success" else "Failure",
                receivedHash = receivedHash,
                receivedLength = receivedLength,
                receivedContentType = receivedContentType,
                signatures = signatures
            )
            currentMessageResponse.update { response }
            resultReady.countDown()
        }
        return Signature(
            name = myServerName,
            hash = receivedHash,
            contentType = receivedContentType,
            contentLength = receivedLength
        )
    }

    override fun sendMessage(body: String): PlayResponse {
        currentMessageWaiting.update { newResponse(body) }
        val contentType = currentRequest.contentType
        val lastNode = nodes.last()
        sendRelayMessage(body, contentType, RegisterResponse(lastNode.host, lastNode.port, timeout, xGameTimestamp, lastNode.uuid), Signatures(listOf()))
        val answered: Boolean = resultReady.await(timeout.toLong(), TimeUnit.SECONDS)
        if (gameFailed >= 10) throw BadRequestException("Perdiste, se llevaron la bocha")
        if (!answered){
            gameFailed += 1
            throw TimeoutException("La red telefónica falló, no me contestaron.")
        }

        resultReady = CountDownLatch(1)

        if(currentMessageResponse.value!!.contentResult == "Failure"){
            gameFailed += 1
            throw FailedMessageException("Yo mandé peras y recibí bananas")}
        if (currentMessageResponse.value!!.signatures.items.size < nodes.size-1) throw SignatureException("Faltan firmas.")
        return currentMessageResponse.value!!
    }

    override fun unregisterNode(uuid: UUID?, salt: String?): String {
        val node = nodes.find { it.uuid == uuid } ?: throw BadRequestException("Nodo no encontrado")
        if (node.salt != salt) throw BadRequestException("Estás tratando de estafarme... El UUID ya existe pero mandaste la clave incorrecta.")

        var index = nodes.indexOf(node)
        if (index < nodes.size - 1) {
            val previousNode = nodes[index + 1]
            val nextNode = nodes[index - 1]
            nodes.remove(node)
            val webClient = WebClient.create("http://${previousNode.host}:${previousNode.port}")
            index += 1
            webClient.post()
                .uri("/reconfigure?uuid=${previousNode.uuid}&salt=${previousNode.salt}&nextHost=${nextNode.host}&nextPort=${nextNode.port}")
                .header("X-Game-Timestamp", index.toString())
                .retrieve()
                .bodyToMono(String::class.java)
                .block() ?: throw BadRequestException("Error unregistering node")
        } else {
            nodes.remove(node)
        }
        return  "Sashay Away"
    }

    override fun reconfigure(
        uuid: UUID?,
        salt: String?,
        nextHost: String?,
        nextPort: Int?,
        xGameTimestamp: Int?
    ): String {
        if (uuid != nextNode!!.uuid || salt != this.salt) throw BadRequestException("Datos inválidos")
        nextNode = RegisterResponse(nextHost!!, nextPort!!, timeout, xGameTimestamp!!, uuid)
        return "へんこうが じゅだくされました"
    }

    internal fun registerToServer(registerHost: String, registerPort: Int) {
        val webClient = WebClient.create("http://$registerHost:$registerPort")
        val response: RegisterResponse = webClient.post()
            .uri("/register-node?host=$myServerHost&port=$myServerPort&name=$myServerName")
            .retrieve()
            .bodyToMono(RegisterResponse::class.java)
            .block() ?: throw RuntimeException("Error registering node")

        nextNode = response
        xGameTimestamp = response.xGameTimestamp

    }

    private fun sendRelayMessage(
        body: String,
        contentType: String,
        relayNode: RegisterResponse,
        signatures: Signatures
    ) {
        val webClient = WebClient.create("http://${relayNode.nextHost}:${relayNode.nextPort}")
        val relayRequest = MultipartBodyBuilder().apply {
            part("message", body, MediaType.parseMediaType(contentType))
            part("signatures", signatures, MediaType.parseMediaType("application/json"))
        }.build()

        val response = webClient.post()
            .uri("/relay")
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(relayRequest))
            .header("X-Game-Timestamp", relayNode.xGameTimestamp.toString())
            .retrieve()
            .onStatus({ it.is4xxClientError || it.is5xxServerError }) {
                val errorWebClient = WebClient.create("http://${registerHost}:${registerPort}")
                errorWebClient.post()
                    .uri("/relay")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromMultipartData(relayRequest))
                    .retrieve()
                    .bodyToMono(Signature::class.java)
                    .block() ?: throw RuntimeException("Error sending relay message")
                throw RuntimeException("Los bits se perdieron en el multiverso")
            }
            .bodyToMono(Signature::class.java)
            .block() ?: throw RuntimeException("Error sending relay message")

        println("Mensaje relayed con éxito. Firma recibida: $response")
    }

    private fun clientSign(message: String, contentType: String): Signature {
        val receivedHash = doHash(message.encodeToByteArray(), salt)
        return Signature(myServerName, receivedHash, contentType, message.length, nextNode!!.uuid)
    }

    private fun newResponse(body: String) = PlayResponse(
        "Unknown",
        currentRequest.contentType,
        body.length,
        doHash(body.encodeToByteArray(), salt),
        "Unknown",
        -1,
        "N/A",
        Signatures(listOf())
    )

    private fun doHash(body: ByteArray, salt: String): String {
        val saltBytes = Base64.getDecoder().decode(salt)
        messageDigest.update(saltBytes)
        val digest = messageDigest.digest(body)
        return Base64.getEncoder().encodeToString(digest)
    }

    companion object {
        fun newSalt(): String = Base64.getEncoder().encodeToString(Random.nextBytes(9))
    }
}