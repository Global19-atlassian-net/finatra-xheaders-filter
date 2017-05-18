package gov.nih.nlm.ncbi.xheadersfilter

import com.twitter.finagle.Service
import com.twitter.finagle.http.{Request, Response}
import com.twitter.finatra.http.exceptions.BadRequestException
import com.twitter.finatra.httpclient.RequestBuilder
import com.twitter.inject.Mockito
import com.twitter.util.{Await, Future}
import org.scalatest.FunSuite

/**
  * Created by anastasia on 4/21/17.
  */
class UnitTests extends FunSuite with Mockito {

  test("No Location header in response and x-script-name header") {
    /* If no "Location" header is present, do nothing */

    // ============== Arrange ==================
    val mockResp = new Response.Ok

    // mock Service with fake response as the return value
    val mockHttp = smartMock[Service[Request, Response]]
    mockHttp.apply(any[Request]) returns Future(mockResp)

    // create request which will go through the client
    val req = RequestBuilder.get("/programs")
      .headers(Map(
        "x-script-name" -> "/too/doo")
      )

    // ============== Act ==================
    val myClient = new XHeadersHttpResponseFilter[Request]()
    val resp: Future[Response] = myClient(req, mockHttp)
    val res: Response = Await.result(resp)

    // ============== Assert ==================
    assert(res.headerMap.contains("Server"))
    assert(res.headerMap.contains("Date"))
    assert(!res.headerMap.contains("Location"))
  }

  test("Location header is preserved if no x-* headers were passed in request") {
    /* If there is "Location" header and no x-* headers were passed, leave it how it is */

    // ============== Arrange ==================
    val mockResp = new Response.Ok
    mockResp.headerMap.set("Location", "http://10.50.3.11:8080/something")

    // mock Service with fake response as the return value
    val mockHttp = smartMock[Service[Request, Response]]
    mockHttp.apply(any[Request]) returns Future(mockResp)

    // create request which will go through the client
    val req = RequestBuilder.get("/programs")

    // ============== Act ==================
    val myClient = new XHeadersHttpResponseFilter[Request]()
    val resp: Future[Response] = myClient(req, mockHttp)
    val res: Response = Await.result(resp)

    // ============== Assert ==================
    assert(res.headerMap.get("Location") == Some("http://10.50.3.11:8080/something"))
  }

  test("Relative URL in Location header and x-script-name header") {

    // ============== Arrange ==================
    val mockResp = new Response.Ok
    mockResp.headerMap.set("Location", "/programs/bla")

    // mock Service with fake response as the return value
    val mockHttp = smartMock[Service[Request, Response]]
    mockHttp.apply(any[Request]) returns Future(mockResp)

    // create request which will go through the client
    val req = RequestBuilder.get("/programs")
      .headers(Map(
        "x-script-name" -> "/too/doo")
      )

    // ============== Act ==================
    val myClient = new XHeadersHttpResponseFilter[Request]()
    val resp: Future[Response] = myClient(req, mockHttp)
    val res: Response = Await.result(resp)

    // ============== Assert ==================
    assert(res.headerMap.get("Location") == Some("/too/doo/programs/bla"))
  }

  test("Relative URL in Location header and x-forwarded-host header") {

    // !!!! Keep URL server relative

    // ============== Arrange ==================
    val mockResp = new Response.Ok
    mockResp.headerMap.set("Location", "/programs/bla")

    // mock Service with fake response as the return value
    val mockHttp = smartMock[Service[Request, Response]]
    mockHttp.apply(any[Request]) returns Future(mockResp)

    // create request which will go through the client
    val req = RequestBuilder.get("/programs")
      .headers(Map(
        "x-forwarded-host" -> "http://too.doo:1331, http://some.boo")
      )

    // ============== Act ==================
    val myClient = new XHeadersHttpResponseFilter[Request]()
    val resp: Future[Response] = myClient(req, mockHttp)
    val res: Response = Await.result(resp)

    // ============== Assert ==================
    assert(res.headerMap.get("Location") == Some("/programs/bla"))
  }

  test("'Partial' Location header and x-forwarded-host header") {

    // Add forwarded host value

    // ============== Arrange ==================
    val mockResp = new Response.Ok
    mockResp.headerMap.set("Location", "bla")

    // mock Service with fake response as the return value
    val mockHttp = smartMock[Service[Request, Response]]
    mockHttp.apply(any[Request]) returns Future(mockResp)

    // create request which will go through the client
    val req = RequestBuilder.get("/programs")
      .headers(Map(
        "x-forwarded-host" -> "too.doo:1331, some.boo",
        "X-Forwarded-Proto" -> "https")
      )

    // ============== Act ==================
    val myClient = new XHeadersHttpResponseFilter[Request]()
    val resp: Future[Response] = myClient(req, mockHttp)
    val res: Response = Await.result(resp)

    // ============== Assert ==================
    assert(res.headerMap.get("Location") == Some("https://too.doo:1331/programs/bla"))
  }

  test("'Partial' Location header and Host header and x-forwarded-host header") {

    // Add forwarded host value

    // ============== Arrange ==================
    val mockResp = new Response.Ok
    mockResp.headerMap.set("Location", "bla")

    // mock Service with fake response as the return value
    val mockHttp = smartMock[Service[Request, Response]]
    mockHttp.apply(any[Request]) returns Future(mockResp)

    // create request which will go through the client
    val req = RequestBuilder.get("/programs")
      .headers(Map(
        "Host" -> "my.host.com",
        "x-forwarded-host" -> "too.doo:1331, some.boo",
        "X-Forwarded-Proto" -> "https")
      )

    // ============== Act ==================
    val myClient = new XHeadersHttpResponseFilter[Request]()
    val resp: Future[Response] = myClient(req, mockHttp)
    val res: Response = Await.result(resp)

    // ============== Assert ==================
    assert(res.headerMap.get("Location") == Some("https://too.doo:1331/programs/bla"))
  }

  test("'Partial' Location header NO Host header and NO x-forwarded-host header") {

    // Add forwarded host value

    // ============== Arrange ==================
    val mockResp = new Response.Ok
    mockResp.headerMap.set("Location", "bla")

    // mock Service with fake response as the return value
    val mockHttp = smartMock[Service[Request, Response]]
    mockHttp.apply(any[Request]) returns Future(mockResp)

    // create request which will go through the client
    val req = RequestBuilder.get("/programs")
      .headers(Map(
        "X-Forwarded-Proto" -> "https")
      )

    // ============== Act ==================
    val myClient = new XHeadersHttpResponseFilter[Request]()
    val resp: Future[Response] = myClient(req, mockHttp)

    // ============== Assert ==================
    assertThrows[BadRequestException] {
      Await.result(resp)
    }
  }

}
