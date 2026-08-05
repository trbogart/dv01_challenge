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

  private def groupKey(record: LoanRecord, groupBy: GroupBy): String = groupBy match
    case GroupBy.State => record.state

  private def computeMetric(records: List[LoanRecord], metric: Metric): BigDecimal = metric match
    case Metric.TotalLoanAmount => records.map(_.loanAmount).sum
