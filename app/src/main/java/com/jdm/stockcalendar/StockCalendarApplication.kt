package com.jdm.stockcalendar

import android.app.Application
import com.jdm.stockcalendar.di.AppContainer

class StockCalendarApplication : Application() {
    val appContainer: AppContainer by lazy { AppContainer(this) }
}
