package com.rj.mqtt_client

import android.os.Bundle
import android.widget.Button

class HelpActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help)

        val btnQuit = findViewById<Button>(R.id.btnQuit)
        btnQuit.setOnClickListener {
            finish()
        }
    }
}