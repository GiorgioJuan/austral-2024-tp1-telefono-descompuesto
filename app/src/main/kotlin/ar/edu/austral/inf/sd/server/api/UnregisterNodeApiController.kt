package ar.edu.austral.inf.sd.server.api

import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
@RequestMapping("\${api.base-path:}")
class UnregisterNodeApiController(@Autowired(required = true) val service: UnregisterNodeApiService) {


    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/unregister-node"],
        produces = ["application/json"]
    )
    fun unregisterNode(
        @Valid @RequestParam(value = "uuid", required = false) uuid: java.util.UUID?,
        @Valid @RequestParam(value = "salt", required = false) salt: kotlin.String?
    ): ResponseEntity<kotlin.String> {
        return ResponseEntity(service.unregisterNode(uuid, salt), HttpStatus.valueOf(202))
    }
}
