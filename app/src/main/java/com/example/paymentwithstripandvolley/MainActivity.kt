package com.example.paymentwithstripandvolley

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.example.paymentwithstripandvolley.databinding.ActivityMainBinding
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import org.json.JSONObject


class MainActivity : AppCompatActivity() {
    private lateinit var paymentSheet: PaymentSheet
    private lateinit var paymentIntentClientSecret: String
    private lateinit var binding: ActivityMainBinding
    private lateinit var customerId: String
    private lateinit var ephemeralKey: String
    private lateinit var clientSecret: String
    private var mount: String = "0"

    // #TODO put your public and secret key
    private val SECRET_KEY = "put you public key"
    private val PUBLISHABLE_KEY = "put you secert key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        Log.d("MainActivity", mount)

        // Initialize Stripe configuration with publishable key
        PaymentConfiguration.init(this, PUBLISHABLE_KEY)

        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        binding.fab.setOnClickListener {
            mount = (( (binding.mount.text.toString().toInt() * 100))).toString()
            postApi() // Start API call when the button is clicked
        }



    }
    private fun onPaymentSheetResult(result: PaymentSheetResult) {
        val resultText = when (result) {
            is PaymentSheetResult.Completed -> "Payment complete!"
            is PaymentSheetResult.Canceled -> "Payment canceled!"
            is PaymentSheetResult.Failed -> "Payment failed! ${result.error.localizedMessage}"
        }
        Toast.makeText(this, resultText, Toast.LENGTH_SHORT).show()
    }


    private fun postApi() {
        val url = "https://api.stripe.com/v1/customers"
        val queue = Volley.newRequestQueue(this)

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                val jsonObject = JSONObject(response)
                customerId = jsonObject.getString("id")
                Log.d("MainActivity", customerId)
                getEphemeralKey(customerId)
            },
            { error ->
                Toast.makeText(this, "Failed to create customer", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf("Authorization" to "Bearer $SECRET_KEY")
            }
        }

        queue.add(stringRequest)
    }

    private fun getEphemeralKey(customerId: String) {
        val url = "https://api.stripe.com/v1/ephemeral_keys"
        val queue = Volley.newRequestQueue(this)

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                val jsonObject = JSONObject(response)
                ephemeralKey = jsonObject.getString("id")
                getClientSecret(customerId, ephemeralKey)
                Log.d("MainActivity", ephemeralKey)
            },
            { error ->
                Toast.makeText(this, "Failed to create ephemeral key", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf(
                    "Authorization" to "Bearer $SECRET_KEY",
                    "Stripe-Version" to "2024-06-20"
                )
            }

            override fun getParams(): Map<String, String> {
                return mapOf("customer" to customerId)
            }
        }

        queue.add(stringRequest)
    }

    private fun getClientSecret(customerId: String, ephemeralKey: String) {
        val url = "https://api.stripe.com/v1/payment_intents"
        val queue = Volley.newRequestQueue(this)

        val stringRequest = object : StringRequest(
            Request.Method.POST, url,
            { response ->
                val jsonObject = JSONObject(response)
                clientSecret = jsonObject.getString("client_secret")
                presentPaymentSheet(clientSecret)
                Log.d("MainActivity", clientSecret)
            },
            { error ->
                Toast.makeText(this, "Failed to create payment intent", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf("Authorization" to "Bearer $SECRET_KEY")
            }

            override fun getParams(): Map<String, String> {
                return mapOf(
                    "customer" to customerId,
                    "amount" to mount,
                    "currency" to "usd",
                    "automatic_payment_methods[enabled]" to "true",
                    "setup_future_usage" to ""
                )
            }
        }

        queue.add(stringRequest)
    }

    private fun presentPaymentSheet(clientSecret: String) {
        val configuration = PaymentSheet.Configuration("Your Business Name")
        paymentSheet.presentWithPaymentIntent(clientSecret, configuration)
    }
}


