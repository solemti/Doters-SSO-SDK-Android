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
    private var flow: String = ""
    private var user: String = ""
    private var extendedToken: Boolean = false

    // URL para carga del SSO Login
    private var SSO_url_login =
        "$urlInit/?clientId=$clientIdInit&clientSecret=$clientSecretInit&language=$languageInit&redirectUri="+schemeInit+"://login&state=$stateInit"
    // URL para carga del SSO SignUp
    private var SSO_url_sign_up =
        "$urlInit/?clientId=$clientIdInit&clientSecret=$clientSecretInit&language=$languageInit&redirectUri="+schemeInit+"://signup&go_to_page=signup&state=$stateInit"
    // URL para carga del SSO Logout
    private var SSO_url_logout = urlInit+"/logout?post_logout_redirect_uri="+schemeInit+"://logout&client_id="+clientIdInit

    private var SSO_url_editProfile = urlInit+"/profile/edit?redirectUri="+schemeInit+"://edit"

    private var SSO_url_deleteAccount = urlInit+"/user/cancel?redirectUri="+schemeInit+"://cancel&clientId=$clientIdInit&clientSecret=$clientSecretInit&originApp=true"

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
        logger.info { "Starting doters sso login v2" }
        loadSSO(this.SSO_url_login+"&extended_token=$extendedToken", context);
    }
    fun signInSAC(context: Context){
        logger.info { "Starting doters sso signInSAC" }
        loadSSO(this.SSO_url_login+"&flow=$flow&user=$user&extended_token=$extendedToken", context);
    }
    fun signUp(context: Context){
        logger.info { "Starting doters sso signUp" }
        loadSSO(this.SSO_url_sign_up, context);
    }
    fun signUpSAC(context: Context){
        logger.info { "Starting doters sso signUpSAC" }
        loadSSO(this.SSO_url_sign_up+"&flow=$flow&user=$user", context);
    }
    fun editProfile(context: Context){
        logger.info { "Starting doters sso editProfile" }
        loadSSO(this.SSO_url_editProfile, context);
    }
    fun deleteAccount(context: Context){
        logger.info { "Starting doters sso SSO_url_deleteAccount" }
        loadSSO(this.SSO_url_deleteAccount, context);
    }
    // Metodo de SDK para login
    fun logOut(context: Context){
        logger.info { "Starting doters sso logout v2" }
        loadSSO(this.SSO_url_logout, context);
    }

    fun userInfo(accessToken: String, callback: UserInfoCallback) {

        val SSOApi = RetrofitHelper.getInstance("${this.apiUrlInit}/").create(SSOAPI::class.java)

        val headers: Map<String, String> = mapOf(
            "Authorization" to "Bearer " + accessToken,
            "Content-Type" to "application/json",
            "X-Channel" to "android"
        )

        GlobalScope.launch {
            try {
                val response = SSOApi.getUserInfo(headers)
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
            } catch (e: Exception) {
                logger.error(e) { "Exception occurred while retrieving user info" }
                callback.processFinish(false, null)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun refreshToken(refreshToken: String, callback: RefreshTokenCallback) {
        val basicToken: String = sdkUtils.generateBasicToken(this.clientIdInit, this.clientSecretInit)
        val headers: Map<String, String> = mapOf(
            "Authorization" to "Basic " + basicToken,
            "Content-Type" to "application/x-www-form-urlencoded",
            "X-Channel" to "android"
        )

        val SSOApi = RetrofitHelper.getInstance("${this.apiUrlInit}/").create(SSOAPI::class.java)

        GlobalScope.launch {
            try {
                val response = SSOApi.refreshToken(headers, refreshToken, "refresh_token")
                if (response != null) {
                    // Checking the results
                    if(response.isSuccessful) {
                        val responseBody: RefreshTokenRequest? = response.body()
                        val refreshTokenResponse: LoginData = LoginData(responseBody?.access_token ?: "",
                            responseBody?.expires_in ?: 0,
                            responseBody?.id_token ?: "", responseBody?.refresh_token ?: "", responseBody?.scope ?: "",
                            responseBody?.token_type ?: "", "", "","",""
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
            } catch (e: Exception) {
                logger.error(e) { "Exception occurred while refreshing token" }
                callback.processFinish(false, null)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun tokenIntrospection(accessToken: String, callback: IntrospectionCallback) {
        val basicToken: String = sdkUtils.generateBasicToken(this.clientIdInit, this.clientSecretInit)
        val headers: Map<String, String> = mapOf(
            "Authorization" to "Basic " + basicToken,
            "Content-Type" to "application/x-www-form-urlencoded",
            "X-Channel" to "android"
        )

        val SSOApi = RetrofitHelper.getInstance("${this.apiUrlInit}/").create(SSOAPI::class.java)

        GlobalScope.launch {
            try {
                val response = SSOApi.tokenintrospection(headers, accessToken, "access_token")
                if (response != null) {
                    // Checking the results
                    if(response.isSuccessful) {
                        val responseBody = response.body()
                        val subData = JSONObject(responseBody?.sub ?: "{}")
                        val subDataResponse: SubData = SubData(
                            (subData.optString("accountId") ?: ""), (subData.optString("user") ?: "")
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
            } catch (e: Exception) {
                logger.error(e) { "Exception occurred while retrieving tokenIntrospection" }
                callback.processFinish(false, null)
            }
        }
    }

    fun parseURI(uri: Uri): LoginData?{
        return sdkUtils.parseURI(uri)
    }

    fun parseURISAC(uri: Uri): LoginDataSAC?{
        return sdkUtils.parseURISAC(uri)
    }

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
    public fun setFlow(flow: String){
        this.flow = flow;
    }
    public fun setUser(user: String){
        this.user = user;
    }
    public fun setExtendedToken(extendedToken: Boolean){
        this.extendedToken = extendedToken;
    }
}