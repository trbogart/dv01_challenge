# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

API MVP is implemented: `GET /api/loans/aggregate` (`dv01api.controllers.LoansController`, routed in `conf/routes`) supports `groupBy` (`state`, `grade`, `yearMonth` — issue_d truncated to month via `YearMonth#toString`'s ISO `yyyy-MM` — or omitted/`*` for no grouping) and `metric` (`totalLoanAmount`, `count`, `averageLoanAmount`, or `averageInterestRate` — averages rounded to 2 decimal places via `LoanAggregationService.average`), with filtering by `grade` (comma-separated), and `dateFrom`/`dateTo` (`yyyy-MM`, inclusive). `groupBy` is `Option[String]` at the route/controller level; a missing value and an explicit `"*"` both resolve to `GroupBy.All` in `AggregateQuery.fromParams`. `GroupBy.All`'s `groupKey` always returns `"*"`, so `LoanAggregationService.aggregate`'s generic filter→group→compute pipeline collapses everything into one `{"key": "*", ...}` bucket with no special-casing needed elsewhere. The `yearMonth` groupBy case is named `GroupBy.IssueMonth` (not `YearMonth`) to avoid shadowing the `java.time.YearMonth` import in the same file. Query parsing/validation lives in `dv01api.model.AggregateQuery.fromParams` (returns `Either[String, AggregateQuery]`, surfaced as `400` with `{"error": "..."}` on failure); filtering/grouping/metric computation lives in `dv01api.service.LoanAggregationService`. `GroupBy` and `Metric` are Scala 3 enums with a `paramName` field so parsing and response serialization stay in sync. Covered by `test/dv01api/model/AggregateQuerySpec.scala` and `test/dv01api/service/LoanAggregationServiceSpec.scala` — `sbt` is now on PATH (see `C:\Users\trbog\bin\sbt.bat`, a wrapper around the IntelliJ Scala plugin's bundled `sbt-launch.jar`), so run `sbt test` to verify rather than taking this note on faith.

Remaining tasks, time permitting:
- Add support for filtering by ficoBand, e.g. 670-739
- Add support for grouping by ficoBand (I may skip this, since I'd have to think about what makes sense)
- Finish documentation and remove TODOs before submitting

## Goal (from README.md)

Build a backend API in Scala that parses and aggregates loan data from a LendingClub-format CSV, exposed dynamically via an API endpoint for visualizing the data. A good starting point for query parameters is date, state, grade, or borrower FICO bands.

- Data file (downloaded separately, not checked into git): `data/LoanStats_securev1_2017Q4.csv`
- Large file (~118k rows, ~100MB). The first line is a banner (`Notes offered by Prospectus...`); the actual CSV header is on line 2 — any parser needs to skip/handle that leading line.
- Columns are the standard LendingClub loan-level export (loan_amnt, term, int_rate, grade/sub_grade, loan_status, issue_d, fico ranges, dti, revolving balances, hardship/settlement fields, etc.) — over 140 columns total.

## Architecture (from README.md)

- Implemented in Scala using Play Framework.
- HTTP layer and routing use Play's built-in `Action`/`conf/routes`, with `play-json` for serialization (no standalone pekko-http server).
- Data is aggregated in code (no database mentioned).
- Tests use MUnit.
- CSV loading (`dv01api.parser.LoanRecordParser`, `dv01api.service.LoanDataLoader`): the data file path comes from the `data_file` key in `conf/application.conf` (plain HOCON, not YAML — Play's config system already handles this natively, so introducing a YAML library wasn't worth it). `LoanDataLoader` is bound as an eager Guice singleton in `dv01api.Module` (registered via `play.modules.enabled` in `application.conf`) so the CSV is parsed once at startup, not lazily on first request. The parser uses `commons-csv` and must tolerate the real file's shape: a banner line before the header, and blank lines plus a "Total amount funded..." summary footer at EOF — both are skipped rather than parsed, by discarding the banner explicitly and treating any row that fails field conversion (via `Try`) as skippable, logged at `warn`.
- Request/performance logging (`dv01api.filters.LoggingFilter`): a `play.api.mvc.Filter` that logs method, path, response status, and elapsed millis (at `info`) for every request. Registered via `dv01api.filters.Filters` (a `DefaultHttpFilters`), wired in via `play.http.filters` in `application.conf`. Uses `org.apache.pekko.stream.Materializer` (not Akka) for the implicit `Filter.mat` — Play 3.0's `org.playframework` line replaced Akka with Apache Pekko.
- Logging config lives in `conf/logback.xml`, not `application.conf` — Play's `logger.<name> = <level>` HOCON keys are deprecated in this Play version and silently have no effect (learned the hard way: root is `WARN` by default, and the `dv01api` logger is explicitly bumped to `INFO` there so both `LoanDataLoader`'s and `LoggingFilter`'s `info` logs are visible).

## Build system

- Scala 3.8.4, sbt 1.12.12 (`project/build.properties`).
- Single root project `dv01_challenge`, defined in `build.sbt` as `(project in file("."))` with `PlayScala` enabled.
- Play's sbt plugin (`org.playframework % sbt-plugin % 3.0.9`) is registered in `project/plugins.sbt`.
- **Play's default layout** (`PlayLayoutPlugin` enabled, not disabled): Scala sources live under `app/` (e.g. `app/dv01api/model/LoanRecord.scala`), config/routes under `conf/`, and test *sources* belong under top-level `test/` (e.g. `test/dv01api/parser/LoanRecordParserSpec.scala`) — not `src/main/scala` / `src/test/scala`. An earlier revision of this project used the standard sbt layout via `.disablePlugins(PlayLayoutPlugin)` with a manual `Compile / resourceDirectory` override to keep `conf/` working; that's been reverted in favor of Play's own layout, so don't reintroduce `src/main/scala` without re-adding both of those.
  - **Resource directories follow sourceDirectory, per config scope**: sbt derives `resourceDirectory` as `sourceDirectory.value / "resources"` *within the same configuration axis* (Compile or Test) — it isn't a fixed default. `PlayLayoutPlugin` explicitly overrides `Compile / sourceDirectory` to `app/` *and* `Compile / resourceDirectory` to `conf/` (a special case, since `conf/` isn't nested under `app/`). For `Test`, it only overrides `Test / sourceDirectory` to `test/` and leaves `resourceDirectory` alone — which means the derived `Test / resourceDirectory` follows to `test/resources/`, not `src/test/resources/`. Test fixtures (e.g. `LoanStats_head.csv`) go in `test/resources/`. (This was gotten wrong twice while wiring up the CSV loader: first assumed `conf/` was untouched by `disablePlugins(PlayLayoutPlugin)` — wrong, caused a "resource not found on classpath: application.conf" error — then assumed `Test/resourceDirectory` stays at the sbt default `src/test/resources` — also wrong, caused a `NullPointerException` in `getResourceAsStream`. Verify against `PlayLayoutPlugin.scala`'s actual settings before changing this again, don't reason from the general Play docs.)
- Explicit deps beyond the Play plugin's defaults: `guice` (Play doesn't bundle a DI implementation), `org.playframework %% play-json` (split out of Play core as its own module), and `org.apache.commons % commons-csv` for CSV parsing.

**Known version risk**: Play 3.0.x officially supports Scala 3.3 LTS; this project pins `scalaVersion` to 3.8.4, which is past what Play has been tested against. If Play-generated code fails to compile, pinning `scalaVersion` down to a 3.3.x LTS release is the likely fix. (Also: sbt 2.0 was tried first and rejected — Play's sbt-plugin isn't published for the sbt2 cross-version at all, hence pinning to sbt 1.12.12; don't bump `project/build.properties` back to sbt 2.x without confirming Play has published a compatible plugin build.)

Common commands:
```
sbt compile                # compile main sources
sbt test                   # run all tests
sbt "testOnly *SomeSpec"   # run a single MUnit test suite
sbt run                    # run the Play app (conf/routes is currently empty — everything 404s until routes/controllers are added)
```

## Git

Do not automatically commit changes.