package com.doters.ssosdk.models

data class LoginData(
    val accessToken:String,
    val expiresIn:Int,
    val idToken:String,
    val refreshToken:String,
    val scope:String,
    val tokenType:String,
    val state:String,
    val error:String,
    val errorDescription:String
) {
    constructor() : this("", 0, "", "", "", "", "", "", "")
}
