package com.venmo.testing

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class PreferencesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fragment)

        PreferencesFragment().apply {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, this, this.tag)
                .commit()
        }
    }

    override fun onNewIntent(newIntent: Intent?) {
        super.onNewIntent(intent)
        intent = newIntent
    }
}
