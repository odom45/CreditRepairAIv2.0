package com.creditrepairai.v2

import android.app.Application
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class CreditRepairApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PDFBoxResourceLoader.init(applicationContext)
    }
}
