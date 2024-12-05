package com.siwiba.wba.model

data class Account(
    val id: String,
    val name: String,
    var jabatan: Int,
    var isAdmin: Boolean,
    val email: String,
    val password: String
)