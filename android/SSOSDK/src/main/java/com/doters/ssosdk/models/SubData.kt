package com.doters.ssosdk.models

data class SubData(
    val customerId: String,
    val user: String,
) {
    constructor() : this("", "")
}
