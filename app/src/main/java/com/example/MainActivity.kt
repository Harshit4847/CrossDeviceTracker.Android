package com.example

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Locale

const val BASE_URL = "https://crossdevicetracker-api-hy-erhyaffahwaufsba.southeastasia-01.azurewebsites.net/"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tokenStore = TokenStore(this)
        val installationIdStore = InstallationIdStore(this)
        installationIdStore.getOrCreateInstallationId()

        if (tokenStore.isLoggedIn()) {
            startActivity(android.content.Intent(this, HomeActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                LoginScreen()
            }
        }
    }
}

@Composable
fun LoginScreen() {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Welcome Back",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Sign in to continue",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Button(
                        onClick = {
                            if (!AuthRequestFactory.isValidLoginInput(email, password)) {
                                Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isLoading = true
                            message = ""

                            CoroutineScope(Dispatchers.IO).launch {
                                val response = try {
                                    val api = createAuthApi()
                                    api.login(LoginRequest(email.trim(), password))
                                } catch (_: Exception) {
                                    null
                                }

                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    if (response != null && response.accessToken.isNotEmpty()) {
                                        tokenStore.saveToken(response.accessToken)
                                        registerDevice(context, response.accessToken)
                                        message = "Login success"
                                        Toast.makeText(context, "Login successful", Toast.LENGTH_SHORT).show()
                                    } else {
                                        message = "Login failed. Check credentials or backend URL."
                                        Toast.makeText(context, "Login failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                        }
                        Text("Login")
                    }

                    if (message.isNotEmpty()) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(top = 12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}

private fun createAuthApi(): AuthApi {
    val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    return retrofit.create(AuthApi::class.java)
}

private fun registerDevice(context: android.content.Context, userToken: String) {
    CoroutineScope(Dispatchers.IO).launch {
        val installationIdStore = InstallationIdStore(context)
        val installationId = installationIdStore.getOrCreateInstallationId()
        val request = DeviceRegistrationRequest(
            deviceName = "Android Device",
            platform = "Android",
            installationId = installationId
        )

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val deviceApi = retrofit.create(DeviceApi::class.java)

        try {
            Log.d("DEVICE_REGISTER", "Starting device registration")
            val response = deviceApi.registerDevice("Bearer $userToken", request)
            Log.d("DEVICE_REGISTER", "Response body = $response")
            val deviceStore = DeviceTokenStore(context)
            deviceStore.saveDeviceToken(response.deviceJwt)
            val savedToken = deviceStore.getDeviceToken()
            Log.d("DEVICE_REGISTER", "Saved device token verification: ${savedToken != null}")
        } catch (e: Exception) {
            Log.e("DEVICE_REGISTER", "Registration failed", e)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MyApplicationTheme {
        LoginScreen()
    }
}