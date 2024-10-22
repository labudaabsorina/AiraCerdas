package com.example.airacerdas

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.airacerdas.Manajemen.*
import com.example.airacerdas.Ob.*

import com.qamar.curvedbottomnaviagtion.CurvedBottomNavigation

class OB : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ob)

        val bottomNavigation = findViewById<CurvedBottomNavigation>(R.id.bottomNavigation)
        bottomNavigation.add(
            CurvedBottomNavigation.Model(1, "Notification", R.drawable.notif)
        )
        bottomNavigation.add(
            CurvedBottomNavigation.Model(2, "Home", R.drawable.ion_home)
        )
        bottomNavigation.add(
            CurvedBottomNavigation.Model(3, "Profile", R.drawable.profile)
        )

        bottomNavigation.setOnClickMenuListener {
            when(it.id){
                1 -> {
                    replaceFragment(NotifOb())
                }
                2 -> {
                    replaceFragment(HomeObnya())
                }
                3 -> {
                    replaceFragment(ProfileOB())
                }
            }
        }

        replaceFragment(HomeObnya()) // Set HomeObnya() as the initial fragment
        bottomNavigation.show(2) // Highlight the Home item in the bottom navigation
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()
    }
}
