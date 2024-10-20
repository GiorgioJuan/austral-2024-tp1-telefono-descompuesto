package ar.edu.austral.inf.sd.server.api

import ar.edu.austral.inf.sd.server.model.RegisterResponse
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

sealed class ApiException(msg: String, val code: Int) : Exception(msg)

class NotFoundException(msg: String, code: Int = HttpStatus.NOT_FOUND.value()) : ApiException(msg, code)
class BadRequestException(msg: String) : ApiException(msg, HttpStatus.BAD_REQUEST.value())
class UnauthorizedException(msg: String) : ApiException(msg, HttpStatus.UNAUTHORIZED.value())
class AcceptedException(val response: RegisterResponse ,msg: String) : ApiException(msg, HttpStatus.ACCEPTED.value())
class SignatureException(msg: String) : ApiException(msg, HttpStatus.INTERNAL_SERVER_ERROR.value())
class FailedMessageException(msg: String) : ApiException(msg, HttpStatus.SERVICE_UNAVAILABLE.value())
class TimeoutException(msg: String) : ApiException(msg, HttpStatus.GATEWAY_TIMEOUT.value())


@ControllerAdvice
class DefaultExceptionHandler {

    @ExceptionHandler(value = [ApiException::class])
    fun onApiException(ex: ApiException, response: HttpServletResponse): Unit =
        response.sendError(ex.code, ex.message)

    @ExceptionHandler(value = [NotImplementedError::class])
    fun onNotImplemented(ex: NotImplementedError, response: HttpServletResponse): Unit =
        response.sendError(HttpStatus.NOT_IMPLEMENTED.value())

    @ExceptionHandler(value = [ConstraintViolationException::class])
    fun onConstraintViolation(ex: ConstraintViolationException, response: HttpServletResponse): Unit =
        response.sendError(HttpStatus.BAD_REQUEST.value(), ex.constraintViolations.joinToString(", ") { it.message })

    @ExceptionHandler(value = [AcceptedException::class])
    fun onAccepted(ex: AcceptedException, response: HttpServletResponse): ResponseEntity<RegisterResponse> =
        ResponseEntity(ex.response, HttpStatus.ACCEPTED)


}
