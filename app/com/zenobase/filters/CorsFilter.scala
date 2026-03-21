package com.zenobase.filters

import play.api.Play
import play.api.mvc._

class CorsFilter extends EssentialFilter {

  private lazy val allowedOrigin: String =
    Play.current.configuration.getString("cors.allowed.origin").getOrElse("https://zenobase.com")

  private val corsHeaders = Seq(
    "Access-Control-Allow-Methods" -> "GET, POST, PUT, DELETE, OPTIONS",
    "Access-Control-Allow-Headers" -> "Authorization, Content-Type",
    "Access-Control-Allow-Credentials" -> "true",
    "Access-Control-Expose-Headers" -> "Link, Location, X-Command-ID, X-Credentials",
    "Access-Control-Max-Age" -> "3600"
  )

  def apply(next: EssentialAction): EssentialAction = EssentialAction { requestHeader =>
    val origin = requestHeader.headers.get("Origin")

    if (requestHeader.method == "OPTIONS") {
      val headers = origin.filter(_ == allowedOrigin).map { o =>
        ("Access-Control-Allow-Origin" -> o) +: corsHeaders
      }.getOrElse(corsHeaders)
      play.api.libs.iteratee.Done(Results.Ok.withHeaders(headers: _*))
    } else {
      next(requestHeader).map { result =>
        origin.filter(_ == allowedOrigin).map { o =>
          result.withHeaders(
            "Access-Control-Allow-Origin" -> o,
            "Access-Control-Allow-Credentials" -> "true"
          )
        }.getOrElse(result)
      }(play.api.libs.concurrent.Execution.defaultContext)
    }
  }
}
