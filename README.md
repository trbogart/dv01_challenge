# Take Home Challenge from dv01

## Summary

Backend API to parses and aggregates loan data.

### Goal

The goal of the challenge is to create a backend that parses and aggregates the data provided above dynamically 
via an API endpoint for visualizing this data. A good starting point for parameters to the API is date, state, 
grade or borrow FICO bands.

## API Endpoint

TODO

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
TODO

## Architecture

- Implemented in Scala using Play Framework
- HTTP layer and routing use Play's built-in `Action`/`conf/routes`, with `play-json` for serialization
- Aggregate data in code (see scalability section)
- MUnit tests

## Scalability
Aggregation is currently done per request, which is O(n) by record count.
This is likely acceptable for a low-volume API at this dataset size (~118k rows).
More scalable solutions appropriate for higher call volume or dataset size include
using a database (e.g. sqlite) or building an index by each possible bucket, 
e.g. grouping loan records by state.

TODO - verify performance

## Other Limitations
- Data is loaded once, at application startup (see the `sbt run` note above for the dev-mode caveat), and remains static
- A real application would need a readiness check to wait for data load finish