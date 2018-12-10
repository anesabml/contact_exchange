package com.app.anesabml.contactexchange

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.app.anesabml.contactexchange.main.SenderActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        findViewById<Button>(R.id.send_button).setOnClickListener {
            startActivity(Intent(this@WelcomeActivity, SenderActivity::class.java))
        }

        findViewById<Button>(R.id.recieve_button).setOnClickListener {
            startActivity(Intent(this@WelcomeActivity, ReceiverActivity::class.java))
        }
    }
}
