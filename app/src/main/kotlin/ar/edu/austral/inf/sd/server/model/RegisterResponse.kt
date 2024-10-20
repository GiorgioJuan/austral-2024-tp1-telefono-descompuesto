package ar.edu.austral.inf.sd.server.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Min

/**
 * 
 * @param nextHost 
 * @param nextPort 
 * @param timeout 
 * @param xGameTimestamp 
 */
data class RegisterResponse(

    @get:JsonProperty("nextHost", required = true) val nextHost: kotlin.String,

    @get:JsonProperty("nextPort", required = true) val nextPort: kotlin.Int,

    @get:Min(0)
    @get:JsonProperty("timeout", required = true) val timeout: kotlin.Int,

    @get:Min(0)
    @get:JsonProperty("xGameTimestamp", required = true) val xGameTimestamp: kotlin.Int,

    @get:JsonProperty("uuid") val uuid: java.util.UUID? = null
    ) {

}

