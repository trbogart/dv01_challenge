package dv01api.service

import dv01api.model.{AggregateBucket, AggregateQuery, FicoBand, GroupBy, LoanRecord, Metric}

import java.time.YearMonth

class LoanAggregationServiceSpec extends munit.FunSuite:

  private def loan(
    state: String,
    grade: String,
    issueDate: YearMonth,
    loanAmount: BigDecimal,
    intRate: BigDecimal = BigDecimal("10.00"),
    ficoLow: Int = 700
  ): LoanRecord =
    LoanRecord(
      issueDate = issueDate,
      state = state,
      grade = grade,
      subGrade = grade + "1",
      ficoLow = ficoLow,
      ficoHigh = ficoLow + 4,
      loanAmount = loanAmount,
      intRate = intRate
    )

  private val records = List(
    loan("CA", "A", YearMonth.of(2018, 1), BigDecimal(10000), BigDecimal("8.00"), ficoLow = 700),
    loan("CA", "B", YearMonth.of(2018, 6), BigDecimal(5000), BigDecimal("9.50"), ficoLow = 675),
    loan("NY", "A", YearMonth.of(2018, 3), BigDecimal(7000), BigDecimal("12.00"), ficoLow = 720),
    loan("NY", "C", YearMonth.of(2017, 12), BigDecimal(3000), BigDecimal("15.25"), ficoLow = 660)
  )

  private def query(
    grades: Set[String] = Set.empty,
    dateFrom: Option[YearMonth] = None,
    dateTo: Option[YearMonth] = None,
    metric: Metric = Metric.TotalLoanAmount,
    groupBy: GroupBy = GroupBy.State,
    ficoBand: Option[FicoBand] = None
  ): AggregateQuery =
    AggregateQuery(groupBy, metric, grades, dateFrom, dateTo, ficoBand)

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

  test("computes count per group") {
    val result = LoanAggregationService.aggregate(records, query(metric = Metric.Count))
    assertEquals(result, List(AggregateBucket("CA", BigDecimal(2)), AggregateBucket("NY", BigDecimal(2))))
  }

  test("computes average loan amount per group") {
    val result = LoanAggregationService.aggregate(records, query(metric = Metric.AverageLoanAmount))
    assertEquals(
      result,
      List(
        AggregateBucket("CA", BigDecimal("7500.00")),
        AggregateBucket("NY", BigDecimal("5000.00"))
      )
    )
  }

  test("computes average interest rate per group, rounded to 2 decimal places") {
    val result = LoanAggregationService.aggregate(records, query(metric = Metric.AverageInterestRate))
    assertEquals(
      result,
      List(
        AggregateBucket("CA", BigDecimal("8.75")),
        AggregateBucket("NY", BigDecimal("13.63"))
      )
    )
  }

  test("GroupBy.All collapses every matching record into a single '*' bucket") {
    val result = LoanAggregationService.aggregate(records, query(groupBy = GroupBy.All))
    assertEquals(result, List(AggregateBucket("*", BigDecimal(25000))))
  }

  test("GroupBy.All still applies filters before collapsing") {
    val result = LoanAggregationService.aggregate(records, query(groupBy = GroupBy.All, grades = Set("A")))
    assertEquals(result, List(AggregateBucket("*", BigDecimal(17000))))
  }

  test("groups by grade") {
    val result = LoanAggregationService.aggregate(records, query(groupBy = GroupBy.Grade))
    assertEquals(
      result,
      List(
        AggregateBucket("A", BigDecimal(17000)),
        AggregateBucket("B", BigDecimal(5000)),
        AggregateBucket("C", BigDecimal(3000))
      )
    )
  }

  test("groups by issue year-month, formatted as ISO yyyy-MM") {
    val result = LoanAggregationService.aggregate(records, query(groupBy = GroupBy.IssueMonth))
    assertEquals(
      result,
      List(
        AggregateBucket("2017-12", BigDecimal(3000)),
        AggregateBucket("2018-01", BigDecimal(10000)),
        AggregateBucket("2018-03", BigDecimal(7000)),
        AggregateBucket("2018-06", BigDecimal(5000))
      )
    )
  }

  test("filters by ficoBand, matched against ficoLow, inclusive on both ends") {
    // ficoLow values: 700, 675, 720, 660 — only 660 falls outside 670-739
    val result = LoanAggregationService.aggregate(
      records,
      query(groupBy = GroupBy.All, ficoBand = Some(FicoBand(670, 739)))
    )
    assertEquals(result, List(AggregateBucket("*", BigDecimal(22000))))
  }

  test("ficoBand boundaries are inclusive") {
    val result = LoanAggregationService.aggregate(
      records,
      query(groupBy = GroupBy.All, ficoBand = Some(FicoBand(660, 660)))
    )
    assertEquals(result, List(AggregateBucket("*", BigDecimal(3000))))
  }
