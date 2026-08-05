package dv01api.model

import play.api.libs.json.{Json, Writes}

/** One `{key, value}` row of an aggregation result, e.g. `{"key": "CA", "value": 48213000}`. */
case class AggregateBucket(key: String, value: BigDecimal)

object AggregateBucket:
  given Writes[AggregateBucket] = Json.writes[AggregateBucket]
