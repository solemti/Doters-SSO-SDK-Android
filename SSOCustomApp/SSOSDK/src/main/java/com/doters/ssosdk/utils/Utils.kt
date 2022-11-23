package com.doters.ssosdk.utils

import android.net.Uri
import android.net.UrlQuerySanitizer
import android.os.Build
import androidx.annotation.RequiresApi
import java.util.*

import com.doters.ssosdk.models.LoginData

class Utils {

    private val sanitizer: UrlQuerySanitizer = UrlQuerySanitizer()

    // Metodo para parsear queryParams de URI recibida por el deepLink
    fun parseURI(uri: Uri?): LoginData {
        var response = LoginData()

        if(uri != null) {
            try {
                sanitizer.setAllowUnregisteredParamaters(true);
                sanitizer.parseUrl(uri.toString());

                var accessToken: String = ""
                var expiresIn: String = "0"
                var idToken: String = ""
                var refreshToken: String = ""
                var scope: String = ""
                var tokenType: String = ""
                var state: String = ""

                if (sanitizer.getValue("access_token") != null) {
                    accessToken = sanitizer.getValue("access_token")
                }
                if (sanitizer.getValue("expires_in") != null) {
                    expiresIn = sanitizer.getValue("expires_in")
                }
                if (sanitizer.getValue("id_token") != null) {
                    idToken = sanitizer.getValue("id_token")
                }
                if (sanitizer.getValue("refresh_token") != null) {
                    refreshToken = sanitizer.getValue("refresh_token")
                }
                if (sanitizer.getValue("scope") != null) {
                    scope = sanitizer.getValue("scope")
                }
                if (sanitizer.getValue("token_type") != null) {
                    tokenType = sanitizer.getValue("token_type")
                }
                if (sanitizer.getValue("state") != null) {
                    state = sanitizer.getValue("state")
                }

                response = LoginData(accessToken, expiresIn.toInt(), idToken, refreshToken, scope, tokenType, state, "", "")
            } catch (e: Exception) {
                response = LoginData("", 0, "", "", "", "", "", "errorException", e.toString())
            }
        } else {
            response = LoginData("", 0, "", "", "", "", "", "URI undefined", "The URI param is required")
        }

        return response
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun generateBasicToken(clientId: String, clientSecret: String): String {
        val decodeBasicToken: String = clientId + ":" + clientSecret
        val basicTokenBA: ByteArray = decodeBasicToken.toByteArray()
        val basicTokenEncode = Base64.getEncoder().encodeToString(basicTokenBA)

        return basicTokenEncode
    }
}