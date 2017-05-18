# XHeaders Filter

Handles "X-Script-Name", "X-Forwarded-Host" and "X-Forwarded-Proto" headers within [Finatra-based](https://twitter.github.io/finatra/) applications.

Supports Scala 2.11 and Finatra 2.9.0.

## Configuration

1. Add this `finatra-xheaders-filter` library into your project's `build.sbt` libraryDependencies list:

```
"gov.nih.nlm.ncbi" %% "finatra-xheaders-filter" % <version>
```


2. Add `XHeadersHttpResponseFilter` to your server **after** the `CommonFilters`:

```$scala
...
import gov.nih.nlm.ncbi.xheadersfilter.XHeadersHttpResponseFilter

class MyServer extends HttpServer{

  override def configureHttp(router: HttpRouter) {
    router
      ...
      .filter[TraceIdMDCFilter[Request, Response]]
      .filter[CommonFilters]
      .filter[XHeadersHttpResponseFilter[Request]]
      ...
  }

}

```

*Note:* It's very important to keep `XHeadersHttpResponseFilter` after [CommonFilters](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/filters/CommonFilters.scala) since it overrides (therefore has to be executed before) [HttpResponseFilter](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/filters/HttpResponseFilter.scala). In the future there might be `NCBICommonFilters` which will contain `XHeadersHttpResponseFilter` instead of [HttpResponseFilter](https://github.com/twitter/finatra/blob/master/http/src/main/scala/com/twitter/finatra/http/filters/HttpResponseFilter.scala).

That's it. The filter will now extract all required headers from the incoming request and will update the response's location if necessary.

## Ignoring "X-Script-Name"

Occasionally, you'll want to bypass "X-Script-Name", keeping it out of a [c.t.f.Response](https://github.com/twitter/finatra/tree/develop/http/src/main/scala/com/twitter/finatra/http/response)'s `Location` http header.  To do so, use the `fixLocation` function from `NCBIResponseUtils` in your Controller:

```$scala
...
import gov.nih.nlm.ncbi.xheadersfilter.NCBIResponseUtils.fixLocation

@Singleton
class MyController @Inject() extends Controller
{

  prefix("/cat") {

    get("/google/?", "Redirect to google") { request: Request =>
      fixLocation(response.movedPermanently.location("https://google.com")) }
    }

    get("/journals/123/?", "Redirect to some journal from another application") {request: Request =>
      fixLocation(response.movedPermanently.location("/journals/123"))
    }
  }
}
```


Questions? Comments? Concerns?

Contact: [@anapana](https://github.com/anapana) or [@edwelker](https://github.com/edwelker)