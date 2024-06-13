package com.creative.androidconcurrencyexamples

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * This activity is used to compare the performance of RxJava and Kotlin Coroutines
 * The scenario is to execute a stub async function 1000 times and measure the time taken
 * The execution thread will be background thread and the result will be updated on the main thread
 * Reference from: [ProAndroidDev](https://proandroiddev.com/kotlin-coroutines-vs-rxjava-an-initial-performance-test-68160cfc6723)
 */
class PerformanceActivity : AppCompatActivity() {

    companion object {
        const val TAG = "PerformanceActivity"
        private const val KOTLIN_COROUTINES_TESTS_TAG = "PerformanceActivity"
        private const val TEST_ITERATIONS_COUNT = 50000
    }

    private var counter = AtomicInteger()
    private var testStartTime: Long = 0
    private val testArray = IntArray(TEST_ITERATIONS_COUNT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfomance)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<Button>(R.id.btn_exec_rx_java).setOnClickListener { onTestRxJavaButtonClick() }
        findViewById<Button>(R.id.btn_exec_kt_coroutines_1).setOnClickListener { onTestCoroutinesLaunchOnEveryUnitButtonClick() }
        findViewById<Button>(R.id.btn_exec_kt_coroutines_2).setOnClickListener { onTestCoroutinesLaunchOnlyOnceButtonClick() }
    }

    private fun onTestCoroutinesLaunchOnEveryUnitButtonClick() {
        val testName = "$TAG : TestCoroutinesLaunchOnEveryUnit"

        startTest(testName)
        for (i in 1..TEST_ITERATIONS_COUNT) {
            lifecycleScope.launch(Dispatchers.Default) {
                async { stubAsyncFunc() }.join()
                withContext(Dispatchers.Main)
                {
                    checkTestEnd(testName)
                }
            }
        }
    }

    private fun onTestCoroutinesLaunchOnlyOnceButtonClick() {
        val testName = "$TAG : TestCoroutinesLaunchOnlyOnce"
        startTest(testName)
        lifecycleScope.launch(Dispatchers.Default) {
            testArray
                .map { stubAsyncFunc() }
                .map {
                    withContext(Dispatchers.Main)
                    {
                        checkTestEnd(testName)
                    }
                }
        }
    }

    @SuppressLint("CheckResult")
    private fun onTestRxJavaButtonClick() {
        val testName = "$TAG : RxJava test"

        startTest(testName)

        val subscribeScheduler = Schedulers.io()
        val observeScheduler = AndroidSchedulers.mainThread()
        for (i in 1..TEST_ITERATIONS_COUNT) {
            Observable.fromCallable {
                stubAsyncFunc()
            }.subscribeOn(subscribeScheduler)
                .observeOn(observeScheduler)
                .subscribe { checkTestEnd(testName) }
        }
    }

    private fun stubAsyncFunc() {
        counter.incrementAndGet()
    }

    private fun startTest(testName: String) {
        testStartTime = System.currentTimeMillis()
        logStart(testName)
    }

    private fun checkTestEnd(testName: String) {
        counter.getAndUpdate {
            if (it == TEST_ITERATIONS_COUNT) {
                val testTime = System.currentTimeMillis() - testStartTime
                logEnd("$testName - ${testTime}ms")
                updateResult("$testName Dataset = $TEST_ITERATIONS_COUNT - ${testTime}ms")
                Log.d(
                    KOTLIN_COROUTINES_TESTS_TAG,
                    "Coroutines test End: ${Thread.currentThread().name}"
                )
                return@getAndUpdate 0
            }

            return@getAndUpdate it
        }
    }

    private fun updateResult(msg: String) {
        this.runOnUiThread {
            findViewById<TextView>(R.id.tv_result).text = msg
        }
    }

    private fun logStart(message: String) {
        Log.i(KOTLIN_COROUTINES_TESTS_TAG, "Start: $message")
    }

    private fun logEnd(message: String) {
        Log.i(KOTLIN_COROUTINES_TESTS_TAG, "End: $message")
    }
}