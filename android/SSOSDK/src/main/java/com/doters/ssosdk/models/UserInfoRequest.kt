package com.doters.ssosdk.models

data class UserInfoRequest(
    val sub: String,
    val email: String,
    val first: String,
    val last: String,
    val title: String,
)
