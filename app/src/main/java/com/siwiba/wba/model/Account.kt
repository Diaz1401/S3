package com.siwiba.wba.model

data class Account(
    val id: String,
    val name: String,
    var jabatan: Int,
    var scopeMode: Int,
    var scopeGaji: Boolean,
    var scopePajak: Boolean,
    var scopePinjaman: Boolean,
    var scopeKas: Boolean,
    var scopeLogistik: Boolean,
    var scopeBpjs: Boolean,
    var scopeTagihan: Boolean,
    var isAdmin: Boolean,
    val email: String,
    val password: String
)