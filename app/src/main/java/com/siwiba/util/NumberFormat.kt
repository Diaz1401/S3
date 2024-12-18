package com.siwiba.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale

/**
 * Utility class for formatting and parsing numbers.
 */
class NumberFormat {
    /**
     * Creates a TextWatcher for formatting currency input in an EditText.
     *
     * @param editText The EditText to attach the TextWatcher to.
     * @return A TextWatcher that formats the input as currency.
     */
    fun createTextWatcher(editText: EditText): TextWatcher {
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                editText.removeTextChangedListener(this)

                val input = s.toString().replace(".", "")
                if (input.isNotEmpty()) {
                    val formatted = formatCurrency(input)
                    editText.setText(formatted)
                    editText.setSelection(formatted.length)
                }

                editText.addTextChangedListener(this)
            }
        }
    }

    /**
     * Formats a string value as currency.
     *
     * @param value The string value to format.
     * @param useAbbreviation Whether to use abbreviations for large numbers (e.g., K, M, B).
     * @return The formatted currency string.
     */
    fun formatCurrency(value: String, useAbbreviation: Boolean = false): String {
        val cleanString = value.replace(".", "")
        if (cleanString.isEmpty()) return ""

        return if (useAbbreviation) {
            val number = cleanString.toLong()
            when {
                number >= 1_000_000_000 -> String.format("%.1fB", number / 1_000_000_000.0)
                number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
                number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
                else -> number.toString()
            }
        } else {
            val decimalFormatSymbols = DecimalFormatSymbols().apply {
                groupingSeparator = '.'
            }
            val decimalFormat = DecimalFormat("#,###", decimalFormatSymbols)
            decimalFormat.format(cleanString.toLong())
        }
    }

    /**
     * Parses a string input as a number.
     *
     * @param input The string input to parse.
     * @return The parsed number, or null if parsing fails.
     */
    fun parseNumber(input: String): Number? {
        val format = NumberFormat.getInstance(Locale.GERMANY)
        return try {
            format.parse(input)
        } catch (e: ParseException) {
            e.printStackTrace()
            null
        }
    }
}