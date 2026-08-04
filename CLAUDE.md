# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

Remaining tasks:
- Parse and load CSV file into LoanRecord list on startup
  - Load a configuration file (YAML if convenient) with a single property (e.g. data_file) that references data/LoanStats_securev1_2017Q4.csv by default
  - Load that file and parse it into a list of LoanRecords
  - Parse unit tests
  - Load unit test (e.g. using LoanStats_head.csv, may move to a test data directory)
- Define API interface, model, dummy implementation, setup routing
- Implement and test single path, no filter (e.g. return count for state)
- Add filter by date range 
- Add additional aggregation metrics (e.g. total loan amount)
- Add additional groupBy options (e.g. grade, fico band, issue month)
- Add additional filters
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

## Build system

- Scala 3.8.4, sbt 1.12.12 (`project/build.properties`).
- Single root project `dv01_challenge`, defined in `build.sbt` as `(project in file("."))` with `PlayScala` enabled.
- Play's sbt plugin (`org.playframework % sbt-plugin % 3.0.9`) is registered in `project/plugins.sbt`.
- **Play's default layout** (`PlayLayoutPlugin` enabled, not disabled): Scala sources live under `app/` (e.g. `app/dv01api/model/LoanRecord.scala`), config/routes under `conf/`, and tests belong under top-level `test/` — not `src/main/scala` / `src/test/scala`. An earlier revision of this project used the standard sbt layout via `.disablePlugins(PlayLayoutPlugin)` with a manual `Compile / resourceDirectory` override to keep `conf/` working; that's been reverted in favor of Play's own layout, so don't reintroduce `src/main/scala` without re-adding both of those.
- Explicit deps beyond the Play plugin's defaults: `guice` (Play doesn't bundle a DI implementation) and `org.playframework %% play-json` (split out of Play core as its own module).

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