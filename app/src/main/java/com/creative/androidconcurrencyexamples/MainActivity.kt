package com.creative.androidconcurrencyexamples

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.creative.androidconcurrencyexamples.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()
    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViewModel()
        setupView()
    }

    private fun setupView() {
        binding?.apply {
            eventHandler = viewModel
        }
    }

    private fun setupViewModel() {
        Log.d("MainActivity", "setupViewModel $viewModel")
        viewModel.messageLiveData.observe(this@MainActivity) {
            binding?.textByLiveData = it
        }
        lifecycleScope.launch {
            viewModel.messageStateFlow.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect {
                binding?.textByStateFlow = it
            }
        }
        lifecycleScope.launch {
            viewModel.messageSharedFlow.flowWithLifecycle(lifecycle, Lifecycle.State.STARTED).collect {
                binding?.textBySharedFlow = it
            }
        }
    }
}