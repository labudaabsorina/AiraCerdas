package com.example.airacerdas

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.example.airacerdas.Manajemen.HomeMene
import com.example.airacerdas.Manajemen.NotifMene
import com.example.airacerdas.Manajemen.ProfileMene
import com.qamar.curvedbottomnaviagtion.CurvedBottomNavigation

class ManajemenFTI : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manajemen_fti)
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
                    replaceFragment(NotifMene())
                }
                2 -> {
                    replaceFragment(HomeMene())
                }
                3 -> {
                    replaceFragment(ProfileMene())
                }
            }
        }
        replaceFragment(HomeMene())
        bottomNavigation.show(2)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer,fragment).commit()


    }

}