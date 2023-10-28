package com.example.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.test.ui.theme.TestTheme
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.security.KeyStore
import java.security.SecureRandom
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        setContent {
            TestTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
        runHttpRequest()


    }

    private fun trustedCertificatesTrustManager(): X509TrustManager {
        val cf = CertificateFactory.getInstance("X.509")
        val caInput: InputStream = resources.openRawResource(R.raw.ca) // Загрузка сертифката
        val ca: Certificate = cf.generateCertificate(caInput)
        caInput.close()

// Создаем keystore содержащий наш доверенный сертификат
        val keyStoreType = KeyStore.getDefaultType()
        val keyStore = KeyStore.getInstance(keyStoreType)
        keyStore.load(null, null)
        keyStore.setCertificateEntry("ca", ca)

        val tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm()
        val tmf = TrustManagerFactory.getInstance(tmfAlgorithm)
        tmf.init(keyStore)

        return tmf.trustManagers[0] as X509TrustManager
    }

    private fun runHttpRequest() {

        val keyStore = KeyStore.getInstance("AndroidCAStore")

        keyStore.load(null, null)

        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(keyStore)
        val trustManagers = trustManagerFactory.trustManagers

        val aliases = keyStore.aliases()

        while (aliases.hasMoreElements()) {
            val alias = aliases.nextElement()

            val certificate: Certificate? = keyStore.getCertificate(alias)
            if (certificate is X509Certificate) {
                val x509certificate = certificate as X509Certificate
                println("Certificate subject: ${x509certificate.subjectDN}")
            }
        }


        val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        keyManagerFactory.init(keyStore, null)

        val keyManagers = keyManagerFactory.keyManagers

        val sslContext = SSLContext.getInstance("TLSv1.3")
        sslContext.init(keyManagers, trustManagers, SecureRandom())

        val client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory
                , trustManagers[0] as X509TrustManager
            )
            .hostnameVerifier { _, _ -> true }
            .build()
//        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://194.87.25.87/json/")
            .build()

        client.newCall(request).enqueue(object: Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")

                    val headers = response.headers()
                    for (i in 0 until headers.size()) {
                        println("${headers.name(i)}: ${headers.value(i)}")
                    }
                    val body = response.body()
                    println(body!!.string())
                }
            }
        })
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TestTheme {
        Greeting("Android")
    }
}