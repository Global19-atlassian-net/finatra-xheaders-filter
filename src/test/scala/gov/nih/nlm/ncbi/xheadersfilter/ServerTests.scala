package gov.nih.nlm.ncbi.xheadersfilter

import com.google.inject.Stage
import com.twitter.finagle.http.Status
import com.twitter.finatra.http.EmbeddedHttpServer
import com.twitter.inject.Test
/**
  * Created by anastasia on 4/25/17.
  */
class ServerTests extends Test {

  val server = new EmbeddedHttpServer(
    stage = Stage.PRODUCTION,
    twitterServer = new XTestServer,
    flags = Map("com.twitter.server.resolverMap" -> "trueviewpm=nil!")
  )

  test("server#startup") {
    server.assertHealthy()
  }

  test("ok") {
    server.httpGet(
      "/test/ok/",
      andExpect = Status.Ok
    )
  }

  test("no x-headers with absolute redirect URL") {
    server.httpGet(
      "/test/absolute-307-temp-redirect",
      andExpect = Status.TemporaryRedirect
    )
  }

  test("no x-headers with server-relative redirect") {
    server.httpGet(
      "/test/absolute-307-temp-redirect",
      andExpect = Status.TemporaryRedirect
    )
  }

  test("x-script-name over http with absolute redirect URL") {
    val response = server.httpGet(
      "/test/absolute-307-temp-redirect",
      headers = Map("x-script-name" -> "/some/foo"),
      andExpect = Status.TemporaryRedirect
    )

    val url = "http://" + server.externalHttpHostAndPort + "/some/foo/test/absolute-307-temp-redirect/target"

    assert(response.headerMap.get("Location").getOrElse("") == url)
  }

  test("x-script-name over https with absolute redirect URL") {
    val response = server.httpGet(
      "/test/absolute-307-temp-redirect",
      headers = Map("x-script-name" -> "/some/foo", "X-Forwarded-Proto" -> "https"),
      andExpect = Status.TemporaryRedirect
    )

    val url = "https://" + server.externalHttpHostAndPort + "/some/foo/test/absolute-307-temp-redirect/target"

    assert(response.headerMap.get("Location").getOrElse("") == url)
  }

  test("x-script-name with server-relative redirect") {
    server.httpGet(
      "/test/server-relative-redirect-to-test-target-created",
      headers = Map("x-script-name" -> "/some/foo"),
      andExpect = Status.Created,
      withLocation = "/some/foo/test/target"
    )
  }

  test("x-forwarded-host over http with absolute redirect URL") {
    val response = server.httpGet(
      "/test/absolute-307-temp-redirect",
      headers = Map("x-forwarded-host" -> "some.com, other.gov"),
      andExpect = Status.TemporaryRedirect,
      withLocation = "/test/absolute-307-temp-redirect/target"
    )

    val url = "http://some.com/test/absolute-307-temp-redirect/target"

    assert(response.headerMap.get("Location").getOrElse("") == url)
  }

  test("x-forwarded-host over https with absolute redirect URL") {
    val response = server.httpGet(
      "/test/absolute-307-temp-redirect",
      headers = Map("x-forwarded-host" -> "some.com, other.gov",
        "X-Forwarded-Proto" -> "https"),
      andExpect = Status.TemporaryRedirect,
      withLocation = "/test/absolute-307-temp-redirect/target"
    )

    val url = "https://some.com/test/absolute-307-temp-redirect/target"

    assert(response.headerMap.get("Location").getOrElse("") == url)
  }

  test("x-forwarded-host with server-relative redirect URL") {
    server.httpGet(
      "/test/server-relative-redirect-to-test-target-created",
      headers = Map("x-forwarded-host" -> "some.com, other.gov"),
      andExpect = Status.Created,
      withLocation = "/test/target"
    )
  }

  test("x-script-name and x-forwarded-host over http with absolute redirect URL") {
    val response = server.httpGet(
      "/test/absolute-307-temp-redirect",
      headers = Map(
        "x-forwarded-host" -> "some.com, other.gov",
        "x-script-name" -> "/bla/foo"),
      andExpect = Status.TemporaryRedirect,
      withLocation = "/bla/foo/test/absolute-307-temp-redirect/target"
    )

    val url = "http://some.com/bla/foo/test/absolute-307-temp-redirect/target"

    assert(response.headerMap.get("Location").getOrElse("") == url)
  }

  test("x-script-name and x-forwarded-host over https with absolute redirect URL") {
    val response = server.httpGet(
      "/test/absolute-307-temp-redirect",
      headers = Map(
        "x-forwarded-host" -> "some.com, other.gov",
        "x-script-name" -> "/bla/foo",
        "x-forwarded-proto" -> "https"
      ),
      andExpect = Status.TemporaryRedirect,
      withLocation = "/bla/foo/test/absolute-307-temp-redirect/target"
    )

    val url = "https://some.com/bla/foo/test/absolute-307-temp-redirect/target"

    assert(response.headerMap.get("Location").getOrElse("") == url)
  }

  test("x-script-name and x-fowarded-host with server-relative redirect URL") {
    server.httpGet(
      "/test/server-relative-redirect-to-test-target-created",
      headers = Map(
        "x-forwarded-host" -> "some.com, other.gov",
        "x-script-name" -> "/bla/foo"),
      andExpect = Status.Created,
      withLocation = "/bla/foo/test/target"
    )
  }

  test("x-script-name not added") {
    val response = server.httpGet(
      "/test/fixed/location",
      headers = Map(
        "x-script-name" -> "/a/b"
      ),
      andExpect = Status.MovedPermanently,
      withLocation = "/journals/123"
    )

    println(response)

    val url = "/journals/123"

    assert(response.headerMap.get("Location").getOrElse("") == url)
  }
}
