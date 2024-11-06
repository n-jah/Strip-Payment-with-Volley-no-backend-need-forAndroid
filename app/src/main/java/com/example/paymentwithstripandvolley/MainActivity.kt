package com.example.paymentwithstripandvolley

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
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
    private lateinit var binding: ActivityMainBinding
    private lateinit var customerId: String
    private lateinit var ephemeralKey: String
    private lateinit var clientSecret: String
    private var mount: String = "0"

    // #TODO Replace with your public and secret key
    private val SECRET_KEY = "put your secert key"
    private val PUBLISHABLE_KEY = "put your publshable key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            enableEdgeToEdge()
        }

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
            try {
                mount = ((binding.mount.text.toString().toInt() * 100)).toString()
                postApi()
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            }
        }

        binding.button.setOnClickListener {
        fetchSavedPaymentMethods()

        }
    }

    private fun onPaymentSheetResult(result: PaymentSheetResult) {
        val resultText = when (result) {
            is PaymentSheetResult.Completed -> "Payment complete!"
            is PaymentSheetResult.Canceled -> "Payment canceled!"
            is PaymentSheetResult.Failed -> "Payment failed! ${result.error.localizedMessage}"

        }
        Log.d("MainActivity", resultText)
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
                Log.e("MainActivity", "Error: ${error.message}")
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf("Authorization" to "Bearer $SECRET_KEY")

            }

            override fun getParams(): Map<String, String> {
                return mapOf("description" to "3Blue1Brown"
                    ,"email" to "william.henry.moody@my-own-personal-domain.com",
                    "name" to "Ahmed Nagah",
                    "phone" to "01000000000",
                    "address[city]" to "Cairo",
                    "address[country]" to "Egypt",
                    "address[line1]" to "Nasr City"
                    ,"address[postal_code]" to "11515",


                )
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
                ephemeralKey = jsonObject.getString("secret")
                getClientSecret(customerId, ephemeralKey)
                Log.d("MainActivity", ephemeralKey)
            },
            { error ->
                Toast.makeText(this, "Failed to create ephemeral key", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Error: ${error.message}")
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
                presentPaymentSheet()
                Log.d("MainActivity", clientSecret)
            },
            { error ->
                Toast.makeText(this, "Failed to create payment intent", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Error: ${error.message}")
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
                    "description" to "payment with strip",
                    "automatic_payment_methods[enabled]" to "true",

                )
            }
        }

        queue.add(stringRequest)
    }

    private fun fetchSavedPaymentMethods() {
        Log.d("MainActivity", customerId)
        val url = "curl -G https://api.stripe.com/v1/customers/cus_NhD8HD2bY8dP3V/cards \\\n"
        val queue = Volley.newRequestQueue(this)

        val stringRequest = object : StringRequest(
            Request.Method.GET, url,
            { response ->
                val jsonObject = JSONObject(response)
                val paymentMethods = jsonObject.getJSONArray("data")

                Log.d("MainActivity", paymentMethods.toString())
                // Iterate through paymentMethods to retrieve card details
                for (i in 0 until paymentMethods.length()) {
                    val method = paymentMethods.getJSONObject(i)
                    val card = method.getJSONObject("card")
                    val brand = card.getString("brand")
                    val last4 = card.getString("last4")
                    val expMonth = card.getInt("exp_month")
                    val expYear = card.getInt("exp_year")

                    Log.d("MainActivity", "Card: $brand ****$last4, Exp: $expMonth/$expYear")
                    // You can now display this card info in your UI
                }
            },
            { error ->
                Toast.makeText(this, "Failed to fetch saved payment methods", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Error: ${error.message}")
            }
        ) {
            override fun getHeaders(): Map<String, String> {
                return mapOf("Authorization" to "Bearer $SECRET_KEY")
            }

        }

        queue.add(stringRequest)
    }

    private fun presentPaymentSheet() {
        paymentSheet.presentWithPaymentIntent(
            clientSecret,
            PaymentSheet.Configuration(
                "Ahmed Nagah",
                PaymentSheet.CustomerConfiguration(
                    customerId,
                    ephemeralKey
                )
            )
        )
        Log.d("MainActivity", clientSecret +"📩"+ customerId + " 🍔"+ephemeralKey)
    }



    @RequiresApi(Build.VERSION_CODES.R)
    private fun enableEdgeToEdge() {
        window.setDecorFitsSystemWindows(false)
    }
}
