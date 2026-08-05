package dv01api.model

import java.time.YearMonth

class AggregateQuerySpec extends munit.FunSuite:

  test("parses a fully populated query") {
    val result = AggregateQuery.fromParams(
      groupBy = Some("state"),
      metric = "totalLoanAmount",
      grade = Some("A,B"),
      dateFrom = Some("2018-01"),
      dateTo = Some("2018-12"),
      ficoBand = Some("670-739")
    )
    assertEquals(
      result,
      Right(
        AggregateQuery(
          groupBy = GroupBy.State,
          metric = Metric.TotalLoanAmount,
          grades = Set("A", "B"),
          dateFrom = Some(YearMonth.of(2018, 1)),
          dateTo = Some(YearMonth.of(2018, 12)),
          ficoBand = Some(FicoBand(670, 739))
        )
      )
    )
  }

  test("defaults grades/dates/ficoBand when optional params are absent") {
    val result = AggregateQuery.fromParams(Some("state"), "totalLoanAmount", None, None, None, None)
    assertEquals(
      result,
      Right(AggregateQuery(GroupBy.State, Metric.TotalLoanAmount, Set.empty, None, None, None))
    )
  }

  test("trims whitespace in the grade list and drops empty entries") {
    val result = AggregateQuery.fromParams(Some("state"), "totalLoanAmount", Some(" A, B ,"), None, None, None)
    assertEquals(result.map(_.grades), Right(Set("A", "B")))
  }

  test("treats a missing groupBy as GroupBy.All (no grouping)") {
    val result = AggregateQuery.fromParams(None, "totalLoanAmount", None, None, None, None)
    assertEquals(result.map(_.groupBy), Right(GroupBy.All))
  }

  test("treats an explicit groupBy=* the same as a missing groupBy") {
    val result = AggregateQuery.fromParams(Some("*"), "totalLoanAmount", None, None, None, None)
    assertEquals(result.map(_.groupBy), Right(GroupBy.All))
  }

  test("rejects an unsupported groupBy") {
    val result = AggregateQuery.fromParams(Some("ficoBand"), "totalLoanAmount", None, None, None, None)
    assert(result.isLeft)
    assert(result.left.exists(_.contains("groupBy")))
  }

  test("parses each supported groupBy") {
    val supported = Seq(
      "state" -> GroupBy.State,
      "grade" -> GroupBy.Grade,
      "yearMonth" -> GroupBy.IssueMonth,
      "*" -> GroupBy.All
    )
    supported.foreach { case (paramName, expected) =>
      val result = AggregateQuery.fromParams(Some(paramName), "totalLoanAmount", None, None, None, None)
      assertEquals(result.map(_.groupBy), Right(expected))
    }
  }

  test("rejects an unsupported metric") {
    val result = AggregateQuery.fromParams(Some("state"), "medianLoanAmount", None, None, None, None)
    assert(result.isLeft)
    assert(result.left.exists(_.contains("metric")))
  }

  test("parses each supported metric") {
    val supported = Seq(
      "totalLoanAmount" -> Metric.TotalLoanAmount,
      "count" -> Metric.Count,
      "averageLoanAmount" -> Metric.AverageLoanAmount,
      "averageInterestRate" -> Metric.AverageInterestRate
    )
    supported.foreach { case (paramName, expected) =>
      val result = AggregateQuery.fromParams(Some("state"), paramName, None, None, None, None)
      assertEquals(result.map(_.metric), Right(expected))
    }
  }

  test("rejects a malformed dateFrom") {
    val result = AggregateQuery.fromParams(Some("state"), "totalLoanAmount", None, Some("not-a-date"), None, None)
    assert(result.isLeft)
    assert(result.left.exists(_.contains("dateFrom")))
  }

  test("rejects a malformed dateTo") {
    val result = AggregateQuery.fromParams(Some("state"), "totalLoanAmount", None, None, Some("2018/12"), None)
    assert(result.isLeft)
    assert(result.left.exists(_.contains("dateTo")))
  }

  test("parses a valid ficoBand") {
    val result = AggregateQuery.fromParams(Some("state"), "totalLoanAmount", None, None, None, Some("670-739"))
    assertEquals(result.map(_.ficoBand), Right(Some(FicoBand(670, 739))))
  }

  test("rejects a ficoBand with low > high") {
    val result = AggregateQuery.fromParams(Some("state"), "totalLoanAmount", None, None, None, Some("739-670"))
    assert(result.isLeft)
    assert(result.left.exists(_.contains("ficoBand")))
  }

  test("rejects a malformed ficoBand") {
    val result = AggregateQuery.fromParams(Some("state"), "totalLoanAmount", None, None, None, Some("not-a-band"))
    assert(result.isLeft)
    assert(result.left.exists(_.contains("ficoBand")))
  }

  test("rejects a ficoBand missing the hyphen") {
    val result = AggregateQuery.fromParams(Some("state"), "totalLoanAmount", None, None, None, Some("670"))
    assert(result.isLeft)
    assert(result.left.exists(_.contains("ficoBand")))
  }
