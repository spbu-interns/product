package org.interns.project

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@Composable
fun App(modifier: Modifier = Modifier) {
    var registered by remember { mutableStateOf(false) }

    Surface(modifier = modifier) {
        if (!registered) {
            RegistrationForm(onRegistered = { registered = true })
        } else {
            RegistrationSuccess()
        }
    }
}