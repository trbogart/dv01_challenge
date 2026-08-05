package dv01api.model

import java.time.YearMonth
import scala.util.Try

enum GroupBy(val paramName: String):
  case State extends GroupBy("state")

object GroupBy:
  def parse(value: String): Option[GroupBy] = values.find(_.paramName == value)

enum Metric(val paramName: String):
  case TotalLoanAmount extends Metric("totalLoanAmount")
  case Count extends Metric("count")
  case AverageLoanAmount extends Metric("averageLoanAmount")
  case AverageInterestRate extends Metric("averageInterestRate")

object Metric:
  def parse(value: String): Option[Metric] = values.find(_.paramName == value)

/**
 * A validated representation of the `/api/loans/aggregate` query string.
 */
case class AggregateQuery(
  groupBy: GroupBy,
  metric: Metric,
  grades: Set[String],
  dateFrom: Option[YearMonth],
  dateTo: Option[YearMonth]
)

object AggregateQuery:

  def fromParams(
    groupBy: String,
    metric: String,
    grade: Option[String],
    dateFrom: Option[String],
    dateTo: Option[String]
  ): Either[String, AggregateQuery] =
    for
      gb <- GroupBy.parse(groupBy).toRight(
        s"Unsupported groupBy '$groupBy'. Supported: ${GroupBy.values.map(_.paramName).mkString(", ")}"
      )
      m <- Metric.parse(metric).toRight(
        s"Unsupported metric '$metric'. Supported: ${Metric.values.map(_.paramName).mkString(", ")}"
      )
      from <- parseYearMonth(dateFrom, "dateFrom")
      to <- parseYearMonth(dateTo, "dateTo")
    yield AggregateQuery(
      groupBy = gb,
      metric = m,
      grades = parseGrades(grade),
      dateFrom = from,
      dateTo = to
    )

  private def parseGrades(grade: Option[String]): Set[String] =
    grade.toSet.flatMap(_.split(",")).map(_.trim).filter(_.nonEmpty)

  private def parseYearMonth(value: Option[String], paramName: String): Either[String, Option[YearMonth]] =
    value match
      case None => Right(None)
      case Some(s) =>
        Try(YearMonth.parse(s)).toOption
          .toRight(s"Invalid $paramName '$s', expected format yyyy-MM")
          .map(Some(_))
