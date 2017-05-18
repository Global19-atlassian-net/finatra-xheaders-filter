package gov.nih.nlm.ncbi.xheadersfilter

import com.twitter.conversions.time._
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finagle.{Service, SimpleFilter}
import com.twitter.finatra.http.HttpHeaders
import com.twitter.util.{ScheduledThreadPoolTimer, Future}
import java.util.{Locale, TimeZone}
import javax.inject.Singleton
import org.apache.commons.lang.time.FastDateFormat
import com.twitter.finatra.http.exceptions.BadRequestException
import com.twitter.util.logging.Logging


object XHeadersHttpResponseFilter extends Logging{

  private [this] val XScriptName = "X-Script-Name"
  private [this] val XForwardedHost = "X-Forwarded-Host"
  private [this] val XForwardedProto = "X-Forwarded-Proto"

  // Ugly but works.
  // Response implicits don't work because Json serializer complains about non-Response type.
  // Messing with serializers seems to be very unpleasant. Header is the easiest solution.
  final val XDisableScriptName = "X-Disable-Script-Name"

  private [this] def xHeaders(request: Request): (Option[String], Option[String], Option[String]) = {

    debug("Extracting x-* headers.")

    val headers = (
      request.headerMap.get(XForwardedProto),
      request.headerMap.get(XForwardedHost) map (forwarded => forwarded.split(",").head),
      request.headerMap.get(XScriptName)
    )

    debug("Headers: " + headers)

    headers
  }

  /** Fully qualified requested URL with ending slash and no query params (suitable for location header creation)
    * This code is stolen from com.twitter.finatra.http.request.RequestUtils. This code might be submitted as a pull-request in the future*/
  def pathUrl(request: Request): String = {

    info("Calculating the full path for relative URL.")

    val (forwardedProto, forwardedHost, scriptName) = xHeaders(request)

    val host = (request.host, forwardedHost) match {
      case (_, Some(fh)) => fh
      case (Some(h), None) => h
      case (None, None) => throw new BadRequestException("Host header and X-Forwarded-Host header not set")
    }

    debug(s"The new host is: $host")

    val scheme = forwardedProto.getOrElse("http")

    val pathWithTrailingSlash = if (request.path.endsWith("/")) request.path else request.path + "/"

    val path = scheme + "://" + host + scriptName.getOrElse("") + pathWithTrailingSlash

    info(s"The full path is: $path")

    path
  }

  def scriptNamePath(request: Request, responseLocation: String): String = {
    val (_, _, scriptName) = xHeaders(request)

    scriptName.getOrElse("")+ responseLocation
  }
}

@Singleton
class XHeadersHttpResponseFilter[R <: Request] extends SimpleFilter[R, Response] with Logging {

  /** The code of this class might seem weird. It's because it's all stolen from HttpResponseFilter.
    * I couldn't inherit from that class because all of it's methods
    * are private, so would need to be overriden anyway.
    *
    * There are two differences from the original filter:
    * - apply now adds script name to the location path if it's applicable
    * - updateLocationHeader checks for X-Forwarded-Proto and X-Forwarded-Host values to construct the
    *   absolute URL for 'partial' locations (aka 'relative', the ones which don't start with slash)
    */

  def apply(request: R, service: Service[R, Response]): Future[Response] = {

    for (response <- service(request)) yield {
      setResponseHeaders(response)
      updateLocationHeader(request, response)
      addScriptName(request, response)
      response
    }
  }

  /* Private */

  /**
    * Sets the HTTP Date and Server header values. If there is no Content-type header in the response, but a non-zero
    * content length, we also set to the generic: application/octet-stream content type on the response.
    *
    * @see Date: <a href="https://tools.ietf.org/html/rfc7231#section-7.1.1.2">Section 7.1.1.2 of RFC 7231</a>
    * @see Server: <a href="https://tools.ietf.org/html/rfc7231#section-7.4.2">Section 7.4.2 of RFC 7231</a>
    * @see Content-Type: <a href="https://tools.ietf.org/html/rfc7231#section-3.1.1.5">Section 3.1.1.5 of RFC 7231</a>
    * @param response - the response on which to set the header values.
    */
  private def setResponseHeaders(response: Response) = {
    response.headerMap.set(HttpHeaders.Server, "Finatra")
    response.headerMap.set(HttpHeaders.Date, currentDateValue)
    if (response.contentType.isEmpty && response.length != 0) {
      // see: https://www.w3.org/Protocols/rfc2616/rfc2616-sec7.html#sec7.2.1
      response.headerMap.set(HttpHeaders.ContentType, "application/octet-stream")
    }
  }

  private def getCurrentDateValue: String = {
    dateFormat.format(System.currentTimeMillis())
  }

  // optimized
  private val dateFormat = FastDateFormat.getInstance(
    HttpHeaders.RFC7231DateFormat,
    TimeZone.getTimeZone("GMT"),
    Locale.ENGLISH)
  @volatile private var currentDateValue: String = getCurrentDateValue
  new ScheduledThreadPoolTimer(
    poolSize = 1,
    name = "HttpDateUpdater",
    makeDaemons = true)
    .schedule(1.second) {
      currentDateValue = getCurrentDateValue
    }

  private def addScriptName(request: R, response: Response) = {
    for (existingLocation <- response.location)
      if (existingLocation.startsWith("/")) {

      info(s"Server-relative location found: figuring out if script name needs to be added.")

      response.headerMap.get(XHeadersHttpResponseFilter.XDisableScriptName) match {
        case None => response.headerMap.set(
          HttpHeaders.Location,
          XHeadersHttpResponseFilter.scriptNamePath(request, existingLocation))
        case _ =>
      }
    }
  }

  private def updateLocationHeader(request: R, response: Response) = {
    for (existingLocation <- response.location) {
      if (!existingLocation.startsWith("http") && !existingLocation.startsWith("/")) {

        info(s"Relative location found: $existingLocation")

        response.headerMap.set(
          HttpHeaders.Location,
          XHeadersHttpResponseFilter.pathUrl(request) + existingLocation)
      }
    }
  }

}
