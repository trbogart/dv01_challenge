package dv01api.service

import dv01api.model.{AggregateBucket, AggregateQuery, GroupBy, LoanRecord, Metric}

/**
 * Applies an [[AggregateQuery]]'s filters, then groups and computes the requested metric.
 */
object LoanAggregationService:

  def aggregate(records: List[LoanRecord], query: AggregateQuery): List[AggregateBucket] =
    records
      .filter(matchesFilters(_, query))
      .groupBy(groupKey(_, query.groupBy))
      .map { case (key, group) => AggregateBucket(key, computeMetric(group, query.metric)) }
      .toList
      .sortBy(_.key)

  private def matchesFilters(record: LoanRecord, query: AggregateQuery): Boolean =
    (query.grades.isEmpty || query.grades.contains(record.grade))
      && query.dateFrom.forall(from => !record.issueDate.isBefore(from))
      && query.dateTo.forall(to => !record.issueDate.isAfter(to))
      && query.ficoBand.forall(band => record.ficoLow >= band.low && record.ficoLow <= band.high)

  private def groupKey(record: LoanRecord, groupBy: GroupBy): String = groupBy match
    case GroupBy.State => record.state
    case GroupBy.Grade => record.grade
    case GroupBy.IssueMonth => record.issueDate.toString // ISO yyyy-MM, matches dateFrom/dateTo's format
    case GroupBy.All => GroupBy.All.paramName

  private def computeMetric(records: List[LoanRecord], metric: Metric): BigDecimal = metric match
    case Metric.TotalLoanAmount => records.map(_.loanAmount).sum
    case Metric.Count => BigDecimal(records.size)
    case Metric.AverageLoanAmount => average(records.map(_.loanAmount))
    case Metric.AverageInterestRate => average(records.map(_.intRate))

  // Only ever called on a groupBy value, which is never empty.
  private def average(values: List[BigDecimal]): BigDecimal =
    (values.sum / values.size).setScale(2, BigDecimal.RoundingMode.HALF_UP)
