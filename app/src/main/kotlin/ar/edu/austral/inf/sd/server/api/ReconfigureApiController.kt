package ar.edu.austral.inf.sd.server.api

import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@RestController
@Validated
@RequestMapping("\${api.base-path:}")
class ReconfigureApiController(@Autowired(required = true) val service: ReconfigureApiService) {


    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/reconfigure"],
        produces = ["application/json"]
    )
    fun reconfigure(
        @Valid @RequestParam(value = "uuid", required = false) uuid: java.util.UUID?,
        @Valid @RequestParam(value = "salt", required = false) salt: kotlin.String?,
        @Valid @RequestParam(value = "nextHost", required = false) nextHost: kotlin.String?,
        @Valid @RequestParam(value = "nextPort", required = false) nextPort: kotlin.Int?,
        @RequestHeader(value = "X-Game-Timestamp", required = false) xGameTimestamp: kotlin.Int?
    ): ResponseEntity<kotlin.String> {
        return ResponseEntity(
            service.reconfigure(uuid, salt, nextHost, nextPort, xGameTimestamp),
            HttpStatus.valueOf(200)
        )
    }
}
