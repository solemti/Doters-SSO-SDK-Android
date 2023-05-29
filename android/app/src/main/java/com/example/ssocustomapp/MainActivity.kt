package com.example.ssocustomapp

import com.doters.ssosdk.models.Introspection // Modelo de datos de la Introspección (Verificación de token)
import com.doters.ssosdk.models.RefreshToken // Modelo de datos del refresh token
import com.doters.ssosdk.models.UserInfoData // Modelo de datos de la información de usuario
import com.doters.ssosdk.models.LoginData // Modelo de datos de los datos de respuesta del login
import com.doters.ssosdk.SSOSDK // Clase para instanciar el SDK del SSO

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.ssocustomapp.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    // params de urls de login y logout (información solo de prueba)
    private val scheme: String = "<SCHEME>" // Scheme con el que esta identificada la app appscheme://
    private val url: String = "<HOST-DOTERS>" // URL de cliente web del SSO https://domain.ex
    private val apiUrl: String = "<HOST-DOTERS>"    // Host de API para hacer logout y consumir servicios de userInfo,
                                                    // introspection y refreshToken https://domain.example
    private val clientId: String = "<CLIENT-ID>" // Client Id para identificar el partner
    private val clientSecret: String = "<CLIENT-SECRET>" // Client Secret necesario para hacer el login
    private val language: String = "<LANGUAGE>" // Codigo de lenguaje a mostrar en la web app del SSO
    private val state: String = "<STATE>" // State necesario para hacer el login

    private var ssosdk = SSOSDK(scheme, url, apiUrl, language, clientId, clientSecret, state) // Instanciación de SDK del SSO

    var parsedlData: LoginData = LoginData() // Inicializacion de variable local donde se aloja la información recibida al hacer el login

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        switchSpinner(false)

        // Aqui se espera recibir los query params del redirecturi despues del login
        val dlData: Uri? = intent.data
        if(dlData != null) parsedlData = ssosdk.parseURI(dlData)!! // Método parseURI para obtener la información de inicio de sesión

        binding.root.findViewById<TextView>(R.id.callback_response).movementMethod = ScrollingMovementMethod()

        if(parsedlData != null) {
            binding.root.findViewById<Button>(R.id.button_logout).isEnabled = true
            binding.root.findViewById<Button>(R.id.button_logout).isClickable = true

            binding.root.findViewById<Button>(R.id.userInfo_btn).isEnabled = true
            binding.root.findViewById<Button>(R.id.userInfo_btn).isClickable = true

            binding.root.findViewById<Button>(R.id.verify_btn).isEnabled = true
            binding.root.findViewById<Button>(R.id.verify_btn).isClickable = true

            binding.root.findViewById<Button>(R.id.refresh_btn).isEnabled = true
            binding.root.findViewById<Button>(R.id.refresh_btn).isClickable = true

            binding.root.findViewById<TextView>(R.id.callback_response).setText(parsedlData.toString())
        }

        // Handler del botón login
        binding.root.findViewById<Button>(R.id.button_login).setOnClickListener {
            ssosdk.signIn(applicationContext) // Inicia carga de SSO para el login
        }

        // Handler del botón logout
        binding.root.findViewById<Button>(R.id.button_logout).setOnClickListener {
            ssosdk.logOut(applicationContext) // Realiza el logout
        }

        // Handler del botón getuserInfo
        binding.root.findViewById<Button>(R.id.userInfo_btn).setOnClickListener {
            val accessToken: String? = this.parsedlData.accessToken

            if (accessToken != null) {
                switchSpinner(true)

                // Método userInfo obtiene la información del usuario logueado
                /*
                * accessToken: AccessToken devuelto despues del login
                * callback: callback instanciado de SSOSDK.UserInfoCallback
                */
                ssosdk.userInfo(accessToken, object : SSOSDK.UserInfoCallback {
                    // Se debe sobre escribir la función processFinish para recibir respuesta del método userInfo
                    /*
                    * success: bandera boolean que indica si fue exitosa o no la respuesta del método
                    * data: Información del usuario, puede ser null
                    */
                    override fun processFinish(success: Boolean, data: UserInfoData?) {
                        switchSpinner(false)
                        if(success) {
                            val responseStr = data.toString()
                            binding.root.findViewById<TextView>(R.id.callback_response).setText(responseStr)
                        } else {
                            println("=====> No se obtuvieron datos de usuario!!!")
                        }
                    }
                })
            }
        }

        // Handler del botón VerifyToken
        binding.root.findViewById<Button>(R.id.verify_btn).setOnClickListener {
            val accessToken: String? = this.parsedlData.accessToken

            if (accessToken != null) {
                switchSpinner(true)

                // Método tokenIntrospection para validar estatus del token
                /*
                * accessToken: AccessToken devuelto despues del login
                * callback: callback instanciado de SSOSDK.IntrospectionCallback
                */
                ssosdk.tokenIntrospection(accessToken, object : SSOSDK.IntrospectionCallback {

                    // Se debe sobre escribir la función processFinish para recibir respuesta del método tokenIntrospection
                    /*
                    * success: bandera boolean que indica si fue exitosa o no la respuesta del método
                    * data: Información del estatus del token, puede ser null
                    */
                    override fun processFinish(success: Boolean, data: Introspection?) {
                        switchSpinner(false)
                        if(success) {
                            val responseStr = data.toString()
                            binding.root.findViewById<TextView>(R.id.callback_response).setText(responseStr)
                        } else {
                            println("=====> Token invalido!!!")
                        }
                    }
                })
            }
        }

        // Handler del botón refreshToken
        binding.root.findViewById<Button>(R.id.refresh_btn).setOnClickListener {
            val refreshToken: String? = this.parsedlData.refreshToken

            if (refreshToken != null) {
                switchSpinner(true)

                // Método refreshToken para actualizar token
                /*
                * refreshToken: refreshToken devuelto despues del login
                * callback: callback instanciado de SSOSDK.RefreshTokenCallback
                */
                ssosdk.refreshToken(refreshToken, object : SSOSDK.RefreshTokenCallback {
                    // Se debe sobre escribir la función processFinish para recibir respuesta del método refreshToken
                    /*
                    * success: bandera boolean que indica si fue exitosa o no la respuesta del método
                    * data: Información del token actualizado, puede ser null
                    */
                    override fun processFinish(success: Boolean, data: RefreshToken?) {
                        switchSpinner(false)
                        if(success) {
                            val responseStr = data.toString()
                            binding.root.findViewById<TextView>(R.id.callback_response).setText(responseStr)
                        } else {
                            println("=====> No fue posible actualizar token!!!")
                        }
                    }
                })
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }

    fun switchSpinner(visible: Boolean){
        binding.root.findViewById<ProgressBar>(R.id.progressBar).visibility = if(visible) View.VISIBLE else View.INVISIBLE
    }
}