package ar.edu.austral.inf.sd.server.api

import ar.edu.austral.inf.sd.server.model.PlayResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController

@RestController
@Validated
@RequestMapping("\${api.base-path:}")
class PlayApiController(@Autowired(required = true) val service: PlayApiService) {


    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/play"],
        produces = ["application/json"]
    )
    fun sendMessage(@Valid @RequestBody body: kotlin.String): ResponseEntity<PlayResponse> {
        return ResponseEntity(service.sendMessage(body), HttpStatus.valueOf(200))
    }
}
