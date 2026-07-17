package com.jdm.stockcalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.jdm.stockcalendar.ui.calendar.CalendarScreen
import com.jdm.stockcalendar.ui.theme.StockCalendarTheme

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { (application as StockCalendarApplication).appContainer }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StockCalendarTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CalendarScreen(
                        repository = appContainer.earningsRepository,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}