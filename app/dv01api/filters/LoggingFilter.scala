package dv01api.filters

import org.apache.pekko.stream.Materializer
import play.api.Logger
import play.api.mvc.{Filter, RequestHeader, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

/** Logs method, path, response status, and elapsed time for every request. */
class LoggingFilter @Inject() (implicit override val mat: Materializer, ec: ExecutionContext) extends Filter:

  private val logger = Logger(getClass)

  override def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] =
    val startTime = System.currentTimeMillis()
    nextFilter(requestHeader).map { result =>
      val elapsedMillis = System.currentTimeMillis() - startTime
      logger.info(s"${requestHeader.method} ${requestHeader.uri} -> ${result.header.status} (${elapsedMillis}ms)")
      result
    }
