package com.siwiba.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

class Format {
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
}