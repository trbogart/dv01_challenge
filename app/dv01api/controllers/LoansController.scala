package dv01api.controllers

import dv01api.model.AggregateQuery
import dv01api.service.{LoanAggregationService, LoanDataLoader}
import play.api.libs.json.Json
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}

import javax.inject.{Inject, Singleton}

@Singleton
class LoansController @Inject() (loanDataLoader: LoanDataLoader, cc: ControllerComponents)
    extends AbstractController(cc):

  /** GET /api/loans/aggregate?groupBy=state&metric=totalLoanAmount&grade=A,B&dateFrom=2018-01&dateTo=2018-12 */
  def aggregate(
    groupBy: String,
    metric: String,
    grade: Option[String],
    dateFrom: Option[String],
    dateTo: Option[String]
  ): Action[AnyContent] = Action {
    AggregateQuery.fromParams(groupBy, metric, grade, dateFrom, dateTo) match
      case Left(error) => BadRequest(Json.obj("error" -> error))
      case Right(query) =>
        val data = LoanAggregationService.aggregate(loanDataLoader.records, query)
        Ok(
          Json.obj(
            "groupBy" -> query.groupBy.paramName,
            "metric" -> query.metric.paramName,
            "data" -> data
          )
        )
  }
