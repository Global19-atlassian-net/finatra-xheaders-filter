package gov.nih.nlm.ncbi.xheadersfilter

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.HttpServer
import com.twitter.finatra.http.filters.{CommonFilters, LoggingMDCFilter, TraceIdMDCFilter}
import com.twitter.finatra.http.routing.HttpRouter

/**
  * Created by anastasia on 4/24/17.
  */

class XTestServer extends HttpServer{

  override def configureHttp(router: HttpRouter) {
    router
      .filter[LoggingMDCFilter[Request, Response]]
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .filter[XHeadersHttpResponseFilter[Request]]
      .add[XTestController]
  }

}
