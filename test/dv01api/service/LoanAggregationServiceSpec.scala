package dv01api.service

import dv01api.model.{AggregateBucket, AggregateQuery, GroupBy, LoanRecord, Metric}

import java.time.YearMonth

class LoanAggregationServiceSpec extends munit.FunSuite:

  private def loan(
    state: String,
    grade: String,
    issueDate: YearMonth,
    loanAmount: BigDecimal
  ): LoanRecord =
    LoanRecord(
      issueDate = issueDate,
      state = state,
      grade = grade,
      subGrade = grade + "1",
      ficoLow = 700,
      ficoHigh = 704,
      loanAmount = loanAmount,
      intRate = BigDecimal("10.00")
    )

  private val records = List(
    loan("CA", "A", YearMonth.of(2018, 1), BigDecimal(10000)),
    loan("CA", "B", YearMonth.of(2018, 6), BigDecimal(5000)),
    loan("NY", "A", YearMonth.of(2018, 3), BigDecimal(7000)),
    loan("NY", "C", YearMonth.of(2017, 12), BigDecimal(3000))
  )

  private def query(
    grades: Set[String] = Set.empty,
    dateFrom: Option[YearMonth] = None,
    dateTo: Option[YearMonth] = None
  ): AggregateQuery =
    AggregateQuery(GroupBy.State, Metric.TotalLoanAmount, grades, dateFrom, dateTo)

  test("groups by state and sums loan amount, with no filters") {
    val result = LoanAggregationService.aggregate(records, query())
    assertEquals(
      result,
      List(
        AggregateBucket("CA", BigDecimal(15000)),
        AggregateBucket("NY", BigDecimal(10000))
      )
    )
  }

  test("filters by grade") {
    val result = LoanAggregationService.aggregate(records, query(grades = Set("A")))
    assertEquals(
      result,
      List(
        AggregateBucket("CA", BigDecimal(10000)),
        AggregateBucket("NY", BigDecimal(7000))
      )
    )
  }

  test("filters by inclusive date range") {
    val result = LoanAggregationService.aggregate(
      records,
      query(dateFrom = Some(YearMonth.of(2018, 1)), dateTo = Some(YearMonth.of(2018, 3)))
    )
    assertEquals(
      result,
      List(
        AggregateBucket("CA", BigDecimal(10000)),
        AggregateBucket("NY", BigDecimal(7000))
      )
    )
  }

  test("excludes records outside the date range boundaries") {
    val result = LoanAggregationService.aggregate(
      records,
      query(dateFrom = Some(YearMonth.of(2018, 1)), dateTo = Some(YearMonth.of(2018, 1)))
    )
    assertEquals(result, List(AggregateBucket("CA", BigDecimal(10000))))
  }

  test("returns no buckets when nothing matches") {
    val result = LoanAggregationService.aggregate(records, query(grades = Set("Z")))
    assertEquals(result, List.empty)
  }
