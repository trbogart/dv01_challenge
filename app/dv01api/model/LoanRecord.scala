package dv01api.model

import java.time.YearMonth

/**
 * Represents a single loan record loaded from the data file.
 *
 * @param issueDate  issue date (issue_d field, converted from MMM-yyyy format with English locale)
 * @param state      US state as unvalidated string, e.g. "CA" (addr_state field) TODO validate?
 * @param grade      grade from A-G (grade field)
 * @param subGrade   sub-grade, e.g. "B5" (sub_grade field)
 * @param ficoLow    low FICO score (fico_range_low field)
 * @param ficoHigh   high FICO score (fico_range_high field)
 * @param loanAmount loan amount, in dollars (loan_amnt field)
 * @param intRate    interest rate as percent (int_rate field,
 *                   with whitespace and % stripped before converting to percent, e.g. "  6.08%" to 6.08)
 */
case class LoanRecord(
                       issueDate: YearMonth, // issue_d, e.g. "Dec-2018"
                       state: String, // addr_state, e.g. "CA"
                       grade: String, // grade, A–G
                       subGrade: String, // sub_grade, e.g. "B5"
                       ficoLow: Int, // fico_range_low
                       ficoHigh: Int, // fico_range_high
                       loanAmount: BigDecimal, // loan_amnt
                       intRate: BigDecimal, // int_rate

                       // TODO may add other fields, e.g. status
                     )
