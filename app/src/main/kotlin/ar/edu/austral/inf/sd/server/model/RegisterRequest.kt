package ar.edu.austral.inf.sd.server.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 
 * @param host 
 * @param port 
 * @param name 
 */
data class RegisterRequest(

    @get:JsonProperty("host", required = true) val host: kotlin.String,

    @get:JsonProperty("port", required = true) val port: kotlin.Int,

    @get:JsonProperty("name", required = true) val name: kotlin.String,

    @get:JsonProperty("uuid", required = false) val uuid: java.util.UUID?,

    @get:JsonProperty("salt", required = false) val salt: kotlin.String?
    ) {

}

