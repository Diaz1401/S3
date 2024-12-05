package com.siwiba.wba.model

data class Account(
    val id: String,
    val name: String,
    val email: String,
    val password: String,
    var isAdmin: Boolean
)