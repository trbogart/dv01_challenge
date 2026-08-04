package dv01api.parser

import dv01api.model.LoanRecord
import org.apache.commons.csv.{CSVFormat, CSVRecord}
import play.api.Logger

import java.io.{BufferedReader, File, FileReader}
import java.nio.charset.StandardCharsets
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.jdk.CollectionConverters.*
import scala.util.control.NonFatal
import scala.util.Using

/**
 * Parses the LendingClub loan data export format: a banner line, then a header line,
 * then one quoted-CSV record per loan, optionally followed by blank lines and a
 * "Total amount funded..." summary footer (both are skipped, not parsed as records).
 */
object LoanRecordParser:

  private val logger = Logger(getClass)

  private val issueDateFormat = DateTimeFormatter.ofPattern("MMM-yyyy", Locale.ENGLISH)

  private val csvFormat = CSVFormat.DEFAULT.builder()
    .setHeader()
    .setSkipHeaderRecord(true)
    .setIgnoreEmptyLines(true)
    .get()

  def parse(file: File): List[LoanRecord] = {
    Using.resource(new BufferedReader(new FileReader(file, StandardCharsets.UTF_8)))(parse)
  }

  def parse(reader: BufferedReader): List[LoanRecord] =
    reader.readLine() // discard the leading banner line; the real header is next
    Using.resource(csvFormat.parse(reader)) { parser =>
      parser.iterator().asScala.flatMap(toLoanRecord).toList
    }

  private def toLoanRecord(record: CSVRecord): Option[LoanRecord] =
    try
      Some(
        LoanRecord(
          issueDate = YearMonth.parse(record.get("issue_d").trim, issueDateFormat),
          state = record.get("addr_state").trim,
          grade = record.get("grade").trim,
          subGrade = record.get("sub_grade").trim,
          ficoLow = record.get("fico_range_low").trim.toInt,
          ficoHigh = record.get("fico_range_high").trim.toInt,
          loanAmount = BigDecimal(record.get("loan_amnt").trim),
          intRate = BigDecimal(record.get("int_rate").trim.stripSuffix("%").trim)
        )
      )
    catch
      case NonFatal(e) =>
        logger.warn(s"Skipping unparseable CSV row ${record.getRecordNumber}: ${e.getMessage}")
        None
