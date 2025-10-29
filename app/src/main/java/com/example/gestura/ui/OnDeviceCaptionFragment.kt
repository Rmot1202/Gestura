package com.example.gestura.ui

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment

class OnDeviceCaptionFragment : Fragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                OnDeviceCaptionScreen()  // <- the Compose screen we built earlier
            }
        }
    }
}
