package com.doters.ssosdk.models

data class LoginDataSAC(
    val activationCode:String,
    val expiresIn:Int,
    val flow:String,
    val sub:String,
    val tokenType:String,
    val state:String,
    val activationCodeSource:String,
    val resultCode:String,
    val error:String,
    val errorDescription:String
) {
    constructor() : this("", 0, "", "", "", "", "","", "", "")
}
