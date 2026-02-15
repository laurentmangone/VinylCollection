package com.example.vinylcollection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.vinylcollection.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Activer edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Gérer les insets pour le FAB
        ViewCompat.setOnApplyWindowInsetsListener(binding.fab) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.translationY = -insets.bottom.toFloat()
            windowInsets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.fabScanBarcode) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.translationY = -insets.bottom.toFloat()
            windowInsets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.fabScanCover) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.translationY = -insets.bottom.toFloat()
            windowInsets
        }

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener {
            val navHost = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_content_main)
                as? androidx.navigation.fragment.NavHostFragment
            val current = navHost?.childFragmentManager?.fragments?.firstOrNull()
            if (current is VinylListFragment) {
                current.openCreateSheet()
            }
        }

        binding.fabScanBarcode.setOnClickListener {
            val navHost = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_content_main)
                as? androidx.navigation.fragment.NavHostFragment
            val current = navHost?.childFragmentManager?.fragments?.firstOrNull()
            if (current is VinylListFragment) {
                current.openCreateSheetScanBarcode()
            }
        }

        binding.fabScanCover.setOnClickListener {
            val navHost = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment_content_main)
                as? androidx.navigation.fragment.NavHostFragment
            val current = navHost?.childFragmentManager?.fragments?.firstOrNull()
            if (current is VinylListFragment) {
                current.openCreateSheetScanCover()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}