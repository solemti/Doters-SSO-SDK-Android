package com.doters.ssosdk

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import com.doters.ssosdk.models.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mu.KotlinLogging

import com.doters.ssosdk.rest.RetrofitHelper
import com.doters.ssosdk.rest.SSOAPI
import com.doters.ssosdk.utils.Utils
import org.json.JSONObject

class SSOSDK constructor(scheme: String, url: String, apiUrl: String, language: String, clientId: String, clientSecret: String,  state: String) : AppCompatActivity() {

    private val logger = KotlinLogging.logger {}

    private val schemeInit: String = scheme
    private val urlInit: String = url
    private val apiUrlInit: String = apiUrl
    private val languageInit: String = language
    private val clientIdInit: String = clientId
    private val clientSecretInit: String = clientSecret
    private val stateInit: String = state

    // URL para carga del SSO Login
    private var SSO_url =
        "$urlInit/?clientId=$clientIdInit&clientSecret=$clientSecretInit&language=$languageInit&redirectUri=$schemeInit&state=$stateInit"
    // URL para carga del SSO Logout
    private var SSO_url_logout = apiUrlInit+"/v1/logout?post_logout_redirect_uri="+schemeInit+"logout&client_id="+clientIdInit

    private val sdkUtils: Utils = Utils()

    // Instanciación de las customTabs
    private val builder = CustomTabsIntent.Builder()

    private lateinit var contexto: Context

    // Nombre del paquete del navegador de chrome mobile
    private var package_name = "com.android.chrome"

    // Interfaz callback de response de peticion de getUserInfo
    interface UserInfoCallback {
        fun processFinish(success: Boolean, data: UserInfoData?)
    }

    interface IntrospectionCallback {
        fun processFinish(success: Boolean, data: IntrospectionData?)
    }

    interface RefreshTokenCallback {
        fun processFinish(success: Boolean, data: LoginData?)
    }

    // Metodo de SDK para login
    fun signIn(context: Context){
        logger.info { "Starting doters sso login" }
        loadSSO(this.SSO_url, context);
    }

    // Metodo de SDK para login
    fun logOut(context: Context){
        logger.info { "Starting doters sso logout" }
        loadSSO(this.SSO_url_logout, context);
    }

    fun userInfo(accessToken: String, callback: UserInfoCallback) {
        val SSOApi = RetrofitHelper.getInstance(this.apiUrlInit).create(SSOAPI::class.java)

        GlobalScope.launch {
            val response = SSOApi.getUserInfo("Bearer " + accessToken)
            if (response != null) {
                // Checking the results
                if(response.isSuccessful) {
                    val responseBody: UserInfoRequest? = response.body()
                    val userInfoResponse: UserInfoData = UserInfoData(responseBody?.sub ?: "",
                        responseBody?.email ?: "",
                        responseBody?.first ?: "", responseBody?.last ?: "", responseBody?.title ?: ""
                    )
                    callback.processFinish(true, userInfoResponse)
                }else {
                    logger.error { "Request to get user info failed, " + (response.errorBody()?.string() ?: "without error info")}
                    callback.processFinish(false, null)
                }
            } else {
                logger.error { "Request to get user info without response"}
                callback.processFinish(false, null)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun refreshToken(refreshToken: String, callback: RefreshTokenCallback) {
        val basicToken: String = sdkUtils.generateBasicToken(this.clientIdInit, this.clientSecretInit)
        val headers: Map<String, String> = mapOf("Authorization" to "Basic " + basicToken, "Content-Type" to "application/x-www-form-urlencoded")

        val SSOApi = RetrofitHelper.getInstance(this.apiUrlInit).create(SSOAPI::class.java)

        GlobalScope.launch {
            val response = SSOApi.refreshToken(headers, refreshToken, "refresh_token")
            if (response != null) {
                // Checking the results
                if(response.isSuccessful) {
                    val responseBody: RefreshTokenRequest? = response.body()
                    val refreshTokenResponse: LoginData = LoginData(responseBody?.access_token ?: "",
                        responseBody?.expires_in ?: 0,
                        responseBody?.id_token ?: "", responseBody?.refresh_token ?: "", responseBody?.scope ?: "",
                        responseBody?.token_type ?: "", "", "", ""
                    )

                    callback.processFinish(true, refreshTokenResponse)
                } else {
                    logger.error { "Request to refresh token failed, " + (response.errorBody()?.string() ?: "without error info")}
                    callback.processFinish(false, null)
                }
            } else {
                logger.error { "Request to refresh token without response"}
                callback.processFinish(false, null)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun tokenIntrospection(accessToken: String, callback: IntrospectionCallback) {
        val basicToken: String = sdkUtils.generateBasicToken(this.clientIdInit, this.clientSecretInit)
        val headers: Map<String, String> = mapOf(
            "Authorization" to "Basic " + basicToken,
            "Content-Type" to "application/x-www-form-urlencoded"
        )

        val SSOApi = RetrofitHelper.getInstance(this.apiUrlInit).create(SSOAPI::class.java)

        GlobalScope.launch {
            val response = SSOApi.tokenintrospection(headers, accessToken, "access_token")
           if (response != null) {
                // Checking the results
                if(response.isSuccessful) {
                    val responseBody = response.body()
                    val subData: JSONObject = JSONObject(responseBody!!.sub)
                    val subDataResponse: SubData = SubData(
                        subData.get("accountId") as String, (subData.get("user") ?: "") as String
                    )
                    val tokenIntrospectionDataResponse: IntrospectionData = IntrospectionData(responseBody?.active ?: false,
                        subDataResponse,
                        responseBody?.client_id ?: "", responseBody?.exp ?: 0, responseBody?.iat ?: 0,
                        responseBody?.iss ?: "", responseBody?.scope ?: "",
                        responseBody?.token_type ?: ""
                    )
                    callback.processFinish(true, tokenIntrospectionDataResponse)
                }else {
                    logger.error { "Request to verify token failed, " + (response.errorBody()?.string() ?: "without error info")}
                    callback.processFinish(false, null)
                }
            } else {
                logger.error { "Request to verify token without response"}
                callback.processFinish(false, null)
            }
        }
    }

    fun parseURI(uri: Uri): LoginData?{
        return sdkUtils.parseURI(uri)
    }

    // Funcion proncipal con logica para carga de customTabs
    private fun loadSSO(redirectURI: String, contexto2: Context){
        // Ejecucion de custom tabs
        contexto = contexto2
        val customBuilder = builder.build()
        val params = CustomTabColorSchemeParams.Builder()
        // params.setToolbarColor((ContextCompat.getColor(contexto, R.color.sso_primary)))
        builder.setDefaultColorSchemeParams(params.build())

        customBuilder.intent.setPackage(package_name)
        customBuilder.intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        customBuilder.launchUrl(contexto, Uri.parse(redirectURI))
    }
}