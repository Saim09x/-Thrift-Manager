package com.thriftmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import com.thriftmanager.ui.ThriftManagerRoot
import com.thriftmanager.ui.theme.ThriftManagerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThriftManagerTheme {
                ThriftManagerRoot()
            }
        }
    }
}
