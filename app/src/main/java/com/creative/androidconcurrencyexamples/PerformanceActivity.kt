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
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

/**
 * This activity is used to compare the performance of RxJava and Kotlin Coroutines
 * The scenario is to execute a stub async function n times and measure the time taken until n times was executed
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
    private var counterEnd = AtomicInteger()

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
        findViewById<Button>(R.id.btn_exec_kt_coroutines_1).setOnClickListener { onTestCoroutinesLaunchOnEveryUnitWithJoinButtonClick() }
        findViewById<Button>(R.id.btn_exec_kt_coroutines_2).setOnClickListener { onTestCoroutinesLaunchOnEveryUnitButtonClick() }
        findViewById<Button>(R.id.btn_exec_kt_coroutines_3).setOnClickListener { onTestCoroutinesLaunchOnlyOnceButtonClick() }
    }

    private fun onTestCoroutinesLaunchOnEveryUnitWithJoinButtonClick() { // this to test the impact of join
        val testName = "$TAG : TestCoroutinesLaunchOnEveryUnitWithJoin"

        startTest(testName)
        for (i in 1..TEST_ITERATIONS_COUNT) {
            lifecycleScope.launch(Dispatchers.IO) {// execute on background thread
                val result = async { stubAsyncFunc() }.await()
                withContext(Dispatchers.Main) // update the result on main thread
                {
                    checkTestEnd(testName, result)
                }
            }
        }
    }

    private fun onTestCoroutinesLaunchOnEveryUnitButtonClick() {
        val testName = "$TAG : TestCoroutinesLaunchOnEveryUnit"

        startTest(testName)
        for (i in 1..TEST_ITERATIONS_COUNT) {
            lifecycleScope.launch(Dispatchers.IO) {// execute on background thread
                val result = stubAsyncFunc()
                withContext(Dispatchers.Main) // update the result on main thread
                {
                    checkTestEnd(testName, result)
                }
            }
        }
    }

    private fun onTestCoroutinesLaunchOnlyOnceButtonClick() {
        val testName = "$TAG : TestCoroutinesLaunchOnlyOnce"
        startTest(testName)
        lifecycleScope.launch(Dispatchers.IO) {// execute on background thread
            testArray
                .map { stubAsyncFunc() }
                .map {
                    withContext(Dispatchers.Main) // update the result on main thread
                    {
                        checkTestEnd(testName, it)
                    }
                }
        }
    }

    @SuppressLint("CheckResult")
    private fun onTestRxJavaButtonClick() {
        val testName = "$TAG : RxJava test"
        startTest(testName)
        for (i in 1..TEST_ITERATIONS_COUNT) {
            Single.create<Int> {
                it.onSuccess(stubAsyncFunc()) // execute on background thread
            }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    checkTestEnd(testName, it)
                }, {}) // update the result on main thread
        }
    }

    private fun stubAsyncFunc(): Int {
        return counter.incrementAndGet()
    }

    private fun startTest(testName: String) {
        testStartTime = System.currentTimeMillis()
        logStart(testName)
    }

    private fun checkTestEnd(testName: String, result: Int) {
        if (result == TEST_ITERATIONS_COUNT) {
            counter.set(0)
            counterEnd.set(0)
            val testTime = System.currentTimeMillis() - testStartTime
            logEnd("$testName - ${testTime}ms")
            updateResult("$testName Dataset = $TEST_ITERATIONS_COUNT - ${testTime}ms")
        }
    }

    private fun updateResult(msg: String) {
        this.runOnUiThread {
            findViewById<TextView>(R.id.tv_result).text = msg
        }
    }

    private fun logStart(message: String) {
        Log.i(KOTLIN_COROUTINES_TESTS_TAG, "Data Set $TEST_ITERATIONS_COUNT Start: $message")
    }

    private fun logEnd(message: String) {
        Log.i(KOTLIN_COROUTINES_TESTS_TAG, "Data Set $TEST_ITERATIONS_COUNT End: $message")
    }
}