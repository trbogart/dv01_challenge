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
TODO

## Tests
TODO

## Architecture

- Implemented in Scala using Play Framework
- HTTP layer is `pekko-http`
  - This is mostly because of familiarity given short timeframe
- Aggregate data in code
- MUnit tests