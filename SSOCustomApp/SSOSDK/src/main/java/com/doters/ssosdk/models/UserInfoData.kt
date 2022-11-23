package com.doters.ssosdk.models

data class UserInfoData(
    val customerId: String,
    val email: String,
    val first: String,
    val last: String,
    val title: String,
) {
    constructor() : this("", "", "", "", "")
}
