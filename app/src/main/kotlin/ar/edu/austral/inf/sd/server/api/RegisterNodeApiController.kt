package ar.edu.austral.inf.sd.server.api

import ar.edu.austral.inf.sd.server.model.RegisterResponse
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.PositiveOrZero
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
class RegisterNodeApiController(@Autowired(required = true) val service: RegisterNodeApiService) {


    @RequestMapping(
        method = [RequestMethod.POST],
        value = ["/register-node"],
        produces = ["application/json"]
    )
    fun registerNode(
        @Valid @RequestParam(value = "host", required = false) @NotBlank host: kotlin.String?,
        @Valid @RequestParam(value = "port", required = false) @PositiveOrZero port: kotlin.Int?,
        @Valid @RequestParam(value = "uuid", required = false) @NotBlank @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\$\n") uuid: java.util.UUID?,
        @Valid @RequestParam(value = "salt", required = false) @NotBlank salt: kotlin.String?,
        @Valid @RequestParam(value = "name", required = false) @NotBlank name: kotlin.String?
    ): ResponseEntity<RegisterResponse> {
        return ResponseEntity(service.registerNode(host, port, uuid, salt, name), HttpStatus.valueOf(200))
    }
}
