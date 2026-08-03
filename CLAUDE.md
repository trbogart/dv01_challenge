# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project state

This is a take-home challenge repo at the **scaffolding stage**: the build is wired up (Play plugin, pekko-http, MUnit) but `src/main/scala` and `src/test/scala` are still empty — no application code, no `app/` or `conf/` directories (the layout Play expects) exist yet.

## Goal (from README.md)

Build a backend API in Scala that parses and aggregates loan data from a LendingClub-format CSV, exposed dynamically via an API endpoint for visualizing the data. A good starting point for query parameters is date, state, grade, or borrower FICO bands.

- Data file (downloaded separately, not checked into git): `data/LoanStats_securev1_2017Q4.csv`
- Large file (~118k rows, ~100MB). The first line is a banner (`Notes offered by Prospectus...`); the actual CSV header is on line 2 — any parser needs to skip/handle that leading line.
- Columns are the standard LendingClub loan-level export (loan_amnt, term, int_rate, grade/sub_grade, loan_status, issue_d, fico ranges, dti, revolving balances, hardship/settlement fields, etc.) — over 140 columns total.

## Architecture (from README.md)

- Implemented in Scala using Play Framework.
- HTTP layer is `pekko-http` — this matches Play 3.0's own default server backend, so no cross-stack (Akka/Pekko) classpath conflicts are expected.
- Data is aggregated in code (no database mentioned).
- Tests use MUnit.

## Build system

- Scala 3.8.4, sbt 1.12.12 (`project/build.properties`).
- Single root project `dv01_challenge`, defined in `build.sbt` as `(project in file("."))` with `PlayScala` enabled.
- Play's sbt plugin (`org.playframework % sbt-plugin % 3.0.9`) is registered in `project/plugins.sbt`.

**Known version risk**: Play 3.0.x officially supports Scala 3.3 LTS; this project pins `scalaVersion` to 3.8.4, which is past what Play has been tested against. If Play-generated code fails to compile, pinning `scalaVersion` down to a 3.3.x LTS release is the likely fix. (Also: sbt 2.0 was tried first and rejected — Play's sbt-plugin isn't published for the sbt2 cross-version at all, hence pinning to sbt 1.12.12; don't bump `project/build.properties` back to sbt 2.x without confirming Play has published a compatible plugin build.)

Common commands:
```
sbt compile                # compile main sources
sbt test                   # run all tests
sbt "testOnly *SomeSpec"   # run a single MUnit test suite
sbt run                    # run the Play app (once app/conf are scaffolded)
```
