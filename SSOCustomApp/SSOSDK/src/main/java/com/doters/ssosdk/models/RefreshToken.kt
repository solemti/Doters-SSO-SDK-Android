package com.doters.ssosdk.models

data class RefreshToken(
    val accessToken: String,
    val expiresIn: Int,
    val idToken: String,
    val refreshToken: String,
    val scope: String,
    val tokenType: String
) {
    constructor() : this("", 0, "", "", "", "")
}
