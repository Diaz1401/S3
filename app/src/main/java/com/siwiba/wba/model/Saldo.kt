package com.siwiba.wba.model

data class Saldo(
    val no: Long = 0,
    val keterangan: String = "",
    val debit: Long = 0,
    val kredit: Long = 0,
    val saldo: Long = 0,
    val editor: String = "",
    val tanggal: String = ""
)