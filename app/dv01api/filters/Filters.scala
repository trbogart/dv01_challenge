package dv01api.filters

import play.api.http.DefaultHttpFilters

import javax.inject.Inject

class Filters @Inject() (loggingFilter: LoggingFilter) extends DefaultHttpFilters(loggingFilter)
