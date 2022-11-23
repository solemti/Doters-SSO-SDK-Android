package com.doters.ssosdk.models

data class IntrospectionData(
    val active: Boolean,
    val subData: SubData,
    val clientId: String,
    val exp: Long,
    val iat: Long,
    val iss: String,
    val scope: String,
    val tokenType: String
) {
    constructor() : this(false, SubData(), "", 0, 0, "", "", "")
}
