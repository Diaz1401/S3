package com.siwiba.wba.model

data class Saldo(
    val no: Int = 0,
    val keterangan: String = "",
    val debit: Int = 0,
    val kredit: Int = 0,
    val saldo: Int = 0,
    val editor: String = "",
    val tanggal: String = ""
)