package com.braintreepayments.demo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import com.braintreepayments.api.uicomponents.compose.CardFields
import com.braintreepayments.api.uicomponents.compose.rememberCardFieldsState

class ComposeCardFieldsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        super.onCreateView(inflater, container, savedInstanceState)
        return ComposeView(requireContext()).apply {
            setContent {
                val cardFieldsState = rememberCardFieldsState()

                Column(modifier = Modifier.padding(16.dp)) {
                    CardFields(state = cardFieldsState)
                }
            }
        }
    }
}
