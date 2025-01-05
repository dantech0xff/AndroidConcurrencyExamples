package com.creative.androidconcurrencyexamples

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ThreadPoolExecutor

/**
 * Created by dan on 23/5/24
 *
 * Copyright © 2024 1010 Creative. All rights reserved.
 */

class MainViewModel : ViewModel(), MainEventHandler {

    private val rxJavaCompositeDisposable = CompositeDisposable()

    private val threadPoolExecutor: ThreadPoolExecutor = ThreadPoolExecutor(
        5, 5, 1,
        java.util.concurrent.TimeUnit.SECONDS,
        java.util.concurrent.LinkedBlockingQueue()
    )

    private val _messageLiveData: MutableLiveData<String> = MutableLiveData("Initial Message LiveData")
    val messageLiveData: LiveData<String> = _messageLiveData

    private val _messageStateFlow: MutableStateFlow<String> = MutableStateFlow("Initial Message StateFlow")
    val messageStateFlow: StateFlow<String> = _messageStateFlow.asStateFlow()

    private val _messageSharedFlow: MutableSharedFlow<String> = MutableSharedFlow()
    val messageSharedFlow = _messageSharedFlow.asSharedFlow()

    override fun onNavigateBackpressureSimulator() {
        viewModelScope.launch {
            _messageSharedFlow.emit("Navigate to Backpressure Simulator")
        }
    }

    override fun onNavigatePerformanceCompare() {
        viewModelScope.launch {
            _messageSharedFlow.emit("Navigate to Performance Compare")
        }
    }

    private val handlerThread = HandlerThread("Example-Using-Thread").apply {
        start()
    }

    override fun onClickExecUsingThread() {
        val handler = Handler(handlerThread.looper)
        handler.post {
            var i = 0
            while (i < 3) {
                Log.d("MainViewModel", "Executed using Thread: ${i++} ${Thread.currentThread().name}")
                Thread.sleep(1000)
            }
        }
    }

    override fun onClickExecUsingThreadPool() {
        threadPoolExecutor.execute {
            var i = 0
            while (i < 3) {
                Log.d("MainViewModel", "Executed using Thread: ${i++} ${Thread.currentThread().name}")
                Thread.sleep(100)
            }
        }
    }

    override fun onClickExecUsingRxJava() {
        exampleRxJavaFilter()
    }

    fun exampleRxJavaMap() {
        Observable.create<Int> {
            for (i in 0..3) {
                it.onNext(i)
            }
            it.onComplete()
        }.observeOn(Schedulers.computation()).map {
            Log.d("MainViewModel", "Executed using RxJava Map Operator: $it ${Thread.currentThread().name}")
            ((it * it).toString() + "-String")
        }.observeOn(AndroidSchedulers.mainThread()).subscribe { it ->
            Log.d("MainViewModel", "Executed using RxJava Map: $it ${Thread.currentThread().name}")
        }.let {
            rxJavaCompositeDisposable.add(it)
        }
    }

    private fun exampleRxJavaFlatMap() {
        Observable.create<Int> {
            for (i in 0..3) {
                it.onNext(i)
                Log.d("MainViewModel", "---")
            }
            it.onComplete()
        }.flatMap {
            Observable.just((it + 1).toString() + "-String", (it + 2).toString()  + "-String")
        }.doOnComplete {
            Log.d("MainViewModel", "Executed using RxJava onComplete ${Thread.currentThread().name}")
        }.subscribe {
            Log.d("MainViewModel", "Executed using RxJava FlatMap: $it ${Thread.currentThread().name}")
        }.let {
            rxJavaCompositeDisposable.add(it)
        }
    }

    private fun exampleRxJavaMerge() {
        val observable1 = Observable.create<Int> {
            for (i in 0..3) {
                it.onNext(i)
                Thread.sleep(100)
            }
            it.onComplete()
        }.subscribeOn(Schedulers.io()).doOnComplete {
            Log.d("MainViewModel", "Executed using RxJava onComplete 1 ${Thread.currentThread().name}")
        }
        val observable2 = Observable.create<Int> {
            for (i in 4..7) {
                it.onNext(i)
                Thread.sleep(1000)
            }
            it.onComplete()
        }.subscribeOn(Schedulers.io()).doOnComplete {
            Log.d("MainViewModel", "Executed using RxJava onComplete 2 ${Thread.currentThread().name}")
        }
        Observable.merge(observable1, observable2)
            .doOnComplete {
                Log.d("MainViewModel", "Executed using RxJava onComplete ${Thread.currentThread().name}")
            }.subscribe {
                Log.d("MainViewModel", "Executed using RxJava Merge: $it ${Thread.currentThread().name}")
            }.let {
                rxJavaCompositeDisposable.add(it)
            }
    }

    private fun exampleRxJavaZip() {
        val observable1 = Observable.create<Int> {
            for (i in 0..100) {
                it.onNext(i)
                Thread.sleep(1000)
            }
            it.onComplete()
        }.subscribeOn(Schedulers.computation())
        val observable2 = Observable.create<Int> {
            for (i in 4..10) {
                it.onNext(i)
                Thread.sleep(100)
            }
            it.onComplete()
        }.subscribeOn(Schedulers.io())
        val observable3 = Observable.create<Int> {
            for (i in 11..12) {
                it.onNext(i)
                Thread.sleep(100)
            }
            it.onComplete()
        }.subscribeOn(Schedulers.io())
        Observable.zip(observable1, observable2, observable3) { t1, t2, t3 -> "$t1 + $t2 + $t3" }
            .doOnComplete {
                Log.d("MainViewModel", "Executed using RxJava onComplete ${Thread.currentThread().name}")
            }.subscribe {
                Log.d("MainViewModel", "Executed using RxJava Zip: $it ${Thread.currentThread().name}")
            }.let {
                rxJavaCompositeDisposable.add(it)
            }
    }

    private fun exampleRxJavaThrottleFirst() {
        Observable.create<Int> {
            for (i in 0..10) {
                it.onNext(i)
                Thread.sleep(100)
            }
            it.onComplete()
        }.throttleFirst(1000, java.util.concurrent.TimeUnit.MILLISECONDS)
            .doOnComplete {
                Log.d("MainViewModel", "Executed using RxJava onComplete ${Thread.currentThread().name}")
            }.subscribe {
                Log.d("MainViewModel", "Executed using RxJava ThrottleFirst: $it ${Thread.currentThread().name}")
            }.let {
                rxJavaCompositeDisposable.add(it)
            }
    }

    private fun exampleRxJavaThrottleLast() {
        Observable.create<Int> {
            for (i in 0..100) {
                it.onNext(i)
                Thread.sleep(100)
            }
            it.onComplete()
        }.throttleLast(200, java.util.concurrent.TimeUnit.MILLISECONDS)
            .doOnComplete {
                Log.d("MainViewModel", "Executed using RxJava onComplete ${Thread.currentThread().name}")
            }.subscribe {
                Log.d("MainViewModel", "Executed using RxJava ThrottleLast: $it ${Thread.currentThread().name}")
            }.let {
                rxJavaCompositeDisposable.add(it)
            }
    }

    private fun exampleRxJavaDebounce() {
        Observable.create<Int> {
            for (i in 0..100) {
                it.onNext(i)
                Thread.sleep(100)
            }
            it.onComplete()
        }.debounce(50, java.util.concurrent.TimeUnit.MILLISECONDS)
            .doOnComplete {
                Log.d("MainViewModel", "Executed using RxJava onComplete ${Thread.currentThread().name}")
            }.subscribe {
                Log.d("MainViewModel", "Executed using RxJava Debounce: $it ${Thread.currentThread().name}")
            }.let {
                rxJavaCompositeDisposable.add(it)
            }
    }

    private fun exampleRxJavaConcat() {
        val observable1 = Observable.create<Int> {
            for (i in 0..3) {
                it.onNext(i)
                Thread.sleep(100)
            }
            it.onComplete()
        }.subscribeOn(Schedulers.io()).doOnComplete {
            Log.d("MainViewModel", "Executed using RxJava onComplete 1 ${Thread.currentThread().name}")
        }
        val observable2 = Observable.create<Int> {
            for (i in 4..7) {
                it.onNext(i)
                Thread.sleep(1000)
            }
            it.onComplete()
        }.subscribeOn(Schedulers.io()).doOnComplete {
            Log.d("MainViewModel", "Executed using RxJava onComplete 2 ${Thread.currentThread().name}")
        }
        Observable.concat(observable1, observable2)
            .doOnComplete {
                Log.d("MainViewModel", "Executed using RxJava onComplete ${Thread.currentThread().name}")
            }.subscribe {
                Log.d("MainViewModel", "Executed using RxJava Concat: $it ${Thread.currentThread().name}")
            }.let {
                rxJavaCompositeDisposable.add(it)
            }
    }

    fun exampleRxJavaFilter() {
        Observable.create<Int> {
            for (i in 0..100) {
                it.onNext(i)
                Thread.sleep(100)
            }
            it.onComplete()
        }.filter {
            it < 10
        }.doOnComplete {
            Log.d("MainViewModel", "Executed using RxJava onComplete ${Thread.currentThread().name}")
        }.subscribe {
            Log.d("MainViewModel", "Executed using RxJava Filter: $it ${Thread.currentThread().name}")
        }.let {
            rxJavaCompositeDisposable.add(it)
        }
    }

    fun exampleRxJava() {
        Observable.create<Int> { emitter ->
            for (i in 0..0) {
                Log.d(
                    "MainViewModel", "Executed using RxJava Emitter: " +
                            "$i ${Thread.currentThread().name}"
                )
                emitter.onNext(i)
                Thread.sleep(100)
            }
            emitter.onComplete()
        }.observeOn(Schedulers.io()).map { result ->
            Log.d("MainViewModel", "Executed using RxJava Map: $result ${Thread.currentThread().name}")
            result * 1000
        }
            .subscribeOn(Schedulers.computation())
            .subscribeOn(Schedulers.io())
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete {
                Log.d("MainViewModel", "Executed using RxJava onComplete ${Thread.currentThread().name}")
            }.subscribe({ i ->
                Log.d("MainViewModel", "Executed using RxJava Subscriber: $i ${Thread.currentThread().name}")
            }, {
                Log.e("MainViewModel", "Error: $it")
            }).also {
                rxJavaCompositeDisposable.add(it)
            }
    }

    override fun onClickExecUsingCoroutines() {
        viewModelScope.launch {
            for (i in 0..100000) {
                _messageLiveData.postValue("Executed using Coroutines: LiveData $i")
                _messageStateFlow.value = "Executed using Coroutines: StateFlow $i"
                _messageSharedFlow.emit("Executed using Coroutines: SharedFlow $i")
                kotlinx.coroutines.delay(100)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        threadPoolExecutor.shutdown()
        rxJavaCompositeDisposable.clear()
        handlerThread.quitSafely()
    }
}

interface MainEventHandler {
    fun onNavigateBackpressureSimulator()
    fun onNavigatePerformanceCompare()
    fun onClickExecUsingThread()
    fun onClickExecUsingThreadPool()
    fun onClickExecUsingRxJava()
    fun onClickExecUsingCoroutines()
}