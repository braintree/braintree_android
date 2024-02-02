package com.venmo.testing

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
}
