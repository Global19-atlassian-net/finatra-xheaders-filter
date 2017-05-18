package gov.nih.nlm.ncbi.xheadersfilter

import javax.inject.{Inject, Singleton}

import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.Controller
import NCBIResponseUtils.fixLocation

/**
  * Created by anastasia on 4/24/17.
  */

@Singleton
class XTestController @Inject() extends Controller {
  prefix("/test") {

    get("/ok/") { request: Request =>
      response.ok.body("Everything is awesome")
    }

    get("/absolute-307-temp-redirect/?") {request: Request =>
      // response returns absolute URL for 'Location' header, i.e.
      // "Location" -> "http://test-url.com:1234/absolute-redirect/target"
      response.temporaryRedirect.location("target")
    }

    get("/server-relative-redirect-to-test-target-created/?") { request: Request =>
      // response returns server-relative URL for "Location" header:
      // "Location" -> "/test/target"
      response
        .created
        .location("/test/target")
    }

    get("/fixed/location/?") {request: Request =>
      // Ugly but works.
      // Implicits don't work because Json serializer complains about non-Response type.
      // Messing with serializers seems to be very unpleasant. Header is the easiest solution.
      val r: Response = fixLocation(response.movedPermanently.location("/journals/123"))

      r
    }
  }
}
