package dv01api.parser

import java.io.{BufferedReader, InputStreamReader}
import java.nio.charset.StandardCharsets
import java.time.YearMonth

class LoanRecordParserSpec extends munit.FunSuite:

  private def openHeadFile(): BufferedReader =
    val stream = getClass.getResourceAsStream("/LoanStats_head.csv")
    new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))

  test("parses data rows, skipping the banner line and trailing summary footer") {
    val records = LoanRecordParser.parse(openHeadFile())
    assertEquals(records.size, 4)
  }

  test("maps CSV fields onto LoanRecord, converting date/percent formats") {
    val records = LoanRecordParser.parse(openHeadFile())
    val first = records.head
    assertEquals(first.issueDate, YearMonth.of(2017, 12))
    assertEquals(first.state, "CA")
    assertEquals(first.grade, "A")
    assertEquals(first.subGrade, "A2")
    assertEquals(first.ficoLow, 780)
    assertEquals(first.ficoHigh, 784)
    assertEquals(first.loanAmount, BigDecimal("40000"))
    assertEquals(first.intRate, BigDecimal("6.08"))
  }

  test("last row parses despite trailing settlement columns present") {
    val records = LoanRecordParser.parse(openHeadFile())
    val last = records.last
    assertEquals(last.state, "NY")
    assertEquals(last.grade, "C")
    assertEquals(last.subGrade, "C1")
  }
