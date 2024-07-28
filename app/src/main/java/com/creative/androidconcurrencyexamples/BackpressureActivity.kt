package com.creative.androidconcurrencyexamples

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.creative.androidconcurrencyexamples.databinding.ActivityBackpressureBinding
import io.reactivex.BackpressureStrategy

class BackpressureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBackpressureBinding
    private val viewModel: BackpressureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBackpressureBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupView()
    }

    private fun setupView() {
        binding.apply {
            btnRxJavaSubjectBackpressure.setOnClickListener { viewModel.onRxJavaSubjectBackpressure()  }
            btnRxJavaObservableBackpressure.setOnClickListener { viewModel.onRxJavaObservableBackpressure()  }
            btnRxJavaFlowableBackpressureMissing.setOnClickListener { viewModel.onRxJavaFlowableBackpressure(BackpressureStrategy.MISSING)  }
            btnRxJavaFlowableBackpressureBuffer.setOnClickListener { viewModel.onRxJavaFlowableBackpressure(BackpressureStrategy.BUFFER)  }
            btnRxJavaFlowableBackpressureLatest.setOnClickListener { viewModel.onRxJavaFlowableBackpressure(BackpressureStrategy.LATEST)  }
            btnRxJavaFlowableBackpressureDrop.setOnClickListener { viewModel.onRxJavaFlowableBackpressure(BackpressureStrategy.DROP)  }
            btnRxJavaFlowableBackpressureError.setOnClickListener { viewModel.onRxJavaFlowableBackpressure(BackpressureStrategy.ERROR)  }
            btnRxJavaHandleObservableBackpressureByFlowable.setOnClickListener { viewModel.onHandleBackpressureObservableByFlowable()  }
            btnRxJavaHandleSubjectBackpressureByFlowable.setOnClickListener { viewModel.onHandleBackpressureSubjectByFlowable() }
        }
    }
}