package dv01api.service

import dv01api.model.LoanRecord
import dv01api.parser.LoanRecordParser
import play.api.{Configuration, Logger}

import java.io.File
import javax.inject.{Inject, Singleton}

/**
 * Loads the configured loan data CSV into memory once, at construction time.
 * Bound as an eager singleton (see [[dv01api.Module]]) so this happens on startup.
 */
@Singleton
class LoanDataLoader @Inject() (configuration: Configuration):

  private val logger = Logger(getClass)

  private val dataFile: File = new File(configuration.get[String]("data_file"))

  val records: List[LoanRecord] =
    logger.info(s"Loading loan data from ${dataFile.getPath}")
    val loaded = LoanRecordParser.parse(dataFile)
    logger.info(s"Loaded ${loaded.size} loan records")
    loaded
