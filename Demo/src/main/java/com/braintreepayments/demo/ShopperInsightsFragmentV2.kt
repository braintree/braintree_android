package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp

class ShopperInsightsFragmentV2 : BaseFragment() {

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                Column(modifier = Modifier.padding(8.dp)) {
                    var emailText by rememberSaveable { mutableStateOf("PR1_merchantname@personal.example.com") }
                    var countryCodeText by rememberSaveable { mutableStateOf("1") }
                    var nationalNumberText by rememberSaveable { mutableStateOf("4082321001") }
                    TextField(
                        value = emailText,
                        onValueChange = { newValue -> emailText = newValue },
                        label = { Text("Email") },
                        modifier = Modifier.padding(4.dp),
                    )
                    TextField(
                        value = countryCodeText,
                        onValueChange = { newValue -> countryCodeText = newValue },
                        label = { Text("Country code") },
                        modifier = Modifier.padding(4.dp)
                    )
                    TextField(
                        value = nationalNumberText,
                        onValueChange = { newValue -> nationalNumberText = newValue },
                        label = { Text("National Number") },
                        modifier = Modifier.padding(4.dp)
                    )

                    Button(onClick = {}) { Text(text = "Create customer session") }
                    Button(onClick = {}) { Text(text = "Update customer session") }
                    Button(onClick = {}) { Text(text = "Get recommendations") }
                }
            }
        }
    }
}
