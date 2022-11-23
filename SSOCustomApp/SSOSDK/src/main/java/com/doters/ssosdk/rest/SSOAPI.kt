package com.doters.ssosdk.rest

import com.doters.ssosdk.models.*
import retrofit2.Response
import retrofit2.http.*

interface SSOAPI {
    @GET("user")
    suspend fun getUserInfo(@Header("Authorization") AuthToken: String) : Response<UserInfoRequest>

    @FormUrlEncoded
    @POST("token/introspection")
    suspend fun tokenintrospection(@HeaderMap headers: Map<String, String>, @Field("token") authToken: String, @Field("token_type_hint") tokenTypeHint: String) : Response<IntrospectionRequest>

    @FormUrlEncoded
    @POST("token")
    suspend fun refreshToken(@HeaderMap headers: Map<String, String>, @Field("refresh_token") authToken: String, @Field("grant_type") tokenTypeHint: String) : Response<RefreshTokenRequest>
}