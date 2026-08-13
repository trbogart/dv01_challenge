# Take Home Challenge from dv01

## Summary

Backend API to parse and aggregate loan data.

### Goal

The goal of the challenge is to create a backend that parses and aggregates the data provided above dynamically 
via an API endpoint for visualizing this data.

## API Endpoint

`GET /api/loans/aggregate`

Query parameters:
- `groupBy` (optional) — one of `state`, `grade`, `yearMonth` (issue_d truncated to month, e.g. `"2018-01"`); omitting it (or passing `groupBy=*` explicitly) skips grouping and aggregates across all matching records into a single `"*"` bucket
- `metric` (required) — one of `totalLoanAmount`, `count`, `averageLoanAmount`, `averageInterestRate` (the latter two rounded to 2 decimal places)
- `grade` (optional) — comma-separated list, e.g. `A,B`; unfiltered if omitted
- `dateFrom`, `dateTo` (optional) — `yyyy-MM`, inclusive on both ends; filters on `issue_d`
   (but note the format difference, e.g. `2017-12` instead of `Dec-2017`)
- `ficoBand` (optional) — `low-high`, e.g. `670-739`, inclusive on both ends; matched against a record's `fico_range_low`, not `fico_range_high`

Example:
```
GET /api/loans/aggregate?groupBy=state&metric=totalLoanAmount&grade=A,B&dateFrom=2017-11&dateTo=2017-12
```
```json
{ "groupBy": "state", "metric": "totalLoanAmount", "data": [{"key": "CA", "value": 48213000}, {"key": "NY", "value": 31200000}] }
```

Without `groupBy` (or with `groupBy=*`), everything collapses into a single `"*"` bucket:
```
GET /api/loans/aggregate?metric=totalLoanAmount
```
```json
{ "groupBy": "*", "metric": "totalLoanAmount", "data": [{"key": "*", "value": 4237913000}] }
```

Invalid/unsupported `groupBy` or `metric` values, malformed dates, or a malformed `ficoBand` return `400` with `{"error": "..."}`.

## Setup
- Download [data](https://drive.google.com/file/d/1RdRVZdy_UYknm0Qr9clXAlQIi0Pts9VI/view?usp=share_link)
- Expand zip file (~118k lines) to `data/LoanStats_securev1_2017Q4.csv`

## Run
```
sbt run
```
Note: in dev mode, Play doesn't build the application (and therefore doesn't load the CSV) until
the first HTTP request comes in — the "Server started" message appears well before that. The
first request after starting will be slower while the ~118k-row file loads; subsequent requests
hit the already-loaded in-memory data. This is a dev-mode-only quirk (see `sbt-plugin`'s hot-reload
behavior); a staged/production run (`sbt stage`) loads the data at process startup instead.

## Tests
```
sbt test
```

## Architecture

- Implemented in Scala using Play Framework
- HTTP layer and routing use Play's built-in `Action`/`conf/routes`, with `play-json` for serialization
- Aggregate data in code (see below for scalability)
- MUnit tests

## Known Limitations / Questions
- Aggregation is currently done per request, which is O(n) by record count.
  - This is likely acceptable for a low-volume API at this dataset size (~118k rows),
  resulting in latency of 50 ms or less on development computer. 
  - More scalable solutions appropriate for higher call volume or dataset size include using a database (e.g. sqlite) 
  or building an index by each possible bucket, e.g. grouping loan records by state.
- Data is loaded once, at startup (see the `sbt run` note above for the dev-mode caveat), and remains static 
  for the lifetime of the application. A real application would likely need to load data dynamically.
- A real application would need a readiness check to wait for the initial data load before taking traffic,
  unless a database was used instead.
- Only supports grouping by a single field.
- FICO filtering based on whether low OR high in band?
- I added unit tests, but not full end-to-end smoke tests.

## Development Notes
- Testing was a bit rushed for the last few features, and more would be required before deployment.

### Additional notes added before review (for discussion, not fully fleshed out)
- Additional features:
  - Return can also return the total count (* group). The client can figure that out, but it might be simpler for some use cases.
  - Filter by state(s) as comma-separated list, similar to grade.
- More thoughts on testing:
  - The Claude test cases actually seemed pretty good, but I would definitely want to review that more thoroughly.
  - I also didn't thoroughly test the actual numbers returned beyond a quick sanity check.
    - One option would be to compare different queries against results calculated in a spreadsheet.
- I mentioned the possibility of an in-memory index:
  - That is possible, and can be very fast if it all fits in memory.
    - Can take intersection of multiple indexes pointing to sets of ids
    - `SortedMap` for ranges or `Map` for discrete buckets 
  - However, the test data is only for a few months, so real data will be much larger.
  - Could also pre-aggregate the data into buckets.
    - Store count and total for each metric with key at most granular level (e.g. month, FICO band, state, grade)
    - Aggregation still needs to group by and sum, but over a smaller number of rows, saving memory and improving performance.
    - Would need some domain knowledge, e.g. how FICO bands actually work.
- An datastore is probably the better option in practice, though.
  - This is a read-heavy, analysis-focused use case (OLAP).
  - Could still partially aggregate data (see previous section).
- One minor thing I noticed with Claude's generated code is that it used `toList.map` instead of `map.toList` in 
 `LoanAggregationService.aggregate`, adding a premature materialization.
- Error is just a string, which should be more structured in a real app.
- Some methods could be refactored to return a `Future`, e.g. `LoanAggregationService.aggregate`.
  - This doesn't really matter with the current implementation, but would be more future-proof.