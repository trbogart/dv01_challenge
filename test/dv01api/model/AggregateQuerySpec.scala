package dv01api.model

import java.time.YearMonth

class AggregateQuerySpec extends munit.FunSuite:

  test("parses a fully populated query") {
    val result = AggregateQuery.fromParams(
      groupBy = "state",
      metric = "totalLoanAmount",
      grade = Some("A,B"),
      dateFrom = Some("2018-01"),
      dateTo = Some("2018-12")
    )
    assertEquals(
      result,
      Right(
        AggregateQuery(
          groupBy = GroupBy.State,
          metric = Metric.TotalLoanAmount,
          grades = Set("A", "B"),
          dateFrom = Some(YearMonth.of(2018, 1)),
          dateTo = Some(YearMonth.of(2018, 12))
        )
      )
    )
  }

  test("defaults grades/dates when optional params are absent") {
    val result = AggregateQuery.fromParams("state", "totalLoanAmount", None, None, None)
    assertEquals(
      result,
      Right(AggregateQuery(GroupBy.State, Metric.TotalLoanAmount, Set.empty, None, None))
    )
  }

  test("trims whitespace in the grade list and drops empty entries") {
    val result = AggregateQuery.fromParams("state", "totalLoanAmount", Some(" A, B ,"), None, None)
    assertEquals(result.map(_.grades), Right(Set("A", "B")))
  }

  test("rejects an unsupported groupBy") {
    val result = AggregateQuery.fromParams("grade", "totalLoanAmount", None, None, None)
    assert(result.isLeft)
    assert(result.left.exists(_.contains("groupBy")))
  }

  test("rejects an unsupported metric") {
    val result = AggregateQuery.fromParams("state", "count", None, None, None)
    assert(result.isLeft)
    assert(result.left.exists(_.contains("metric")))
  }

  test("rejects a malformed dateFrom") {
    val result = AggregateQuery.fromParams("state", "totalLoanAmount", None, Some("not-a-date"), None)
    assert(result.isLeft)
    assert(result.left.exists(_.contains("dateFrom")))
  }

  test("rejects a malformed dateTo") {
    val result = AggregateQuery.fromParams("state", "totalLoanAmount", None, None, Some("2018/12"))
    assert(result.isLeft)
    assert(result.left.exists(_.contains("dateTo")))
  }
