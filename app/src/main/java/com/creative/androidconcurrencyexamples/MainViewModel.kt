package com.creative.androidconcurrencyexamples

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.internal.operators.flowable.FlowableAll
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * Created by dan on 23/5/24
 *
 * Copyright © 2024 1010 Creative. All rights reserved.
 */

class MainViewModel : ViewModel(), MainEventHandler {

    private val ioDispatcher = Dispatchers.IO
    private val rxJavaCompositeDisposable = CompositeDisposable()

    private val threadPoolExecutor: ThreadPoolExecutor = ThreadPoolExecutor(
        1, 1, 1,
        java.util.concurrent.TimeUnit.SECONDS,
        java.util.concurrent.LinkedBlockingQueue()
    )

    private val _messageLiveData: MutableLiveData<String> = MutableLiveData("Initial Message LiveData")
    val messageLiveData: LiveData<String> = _messageLiveData

    private val _messageStateFlow: MutableStateFlow<String> = MutableStateFlow("Initial Message StateFlow")
    val messageStateFlow: StateFlow<String> = _messageStateFlow.asStateFlow()

    private val _messageSharedFlow: MutableSharedFlow<String> = MutableSharedFlow()
    val messageSharedFlow = _messageSharedFlow.asSharedFlow()

    override fun onNavigatePerformanceCompare() {
        viewModelScope.launch {
            _messageSharedFlow.emit("Navigate to Performance Compare")
        }
    }

    override fun onClickExecUsingThread() {
        Thread {
            for (i in 0..10) {
                _messageLiveData.postValue("Executed using Thread: LiveData $i")
                _messageStateFlow.value = "Executed using Thread: StateFlow $i"
                viewModelScope.launch(ioDispatcher) {
                    _messageSharedFlow.emit("Executed using Thread: SharedFlow $i")
                }
                Thread.sleep(1000)
            }
        }.start()
    }

    override fun onClickExecUsingThreadPool() {
        threadPoolExecutor.execute {
            for (i in 0..10) {
                _messageLiveData.postValue("Executed using ThreadPool: LiveData $i")
                _messageStateFlow.value = "Executed using ThreadPool: StateFlow $i"
                viewModelScope.launch(ioDispatcher) {
                    _messageSharedFlow.emit("Executed using ThreadPool: SharedFlow $i")
                }
                Thread.sleep(1000)
            }
        }
    }

    override fun onClickExecUsingRxJava() {
        Flowable.create({ emitter ->
            for (i in 0..10) {
                emitter.onNext(i)
                Thread.sleep(1000)
            }
            emitter.onComplete()
        }, BackpressureStrategy.BUFFER)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ i ->
                _messageLiveData.postValue("Executed using ThreadPool: LiveData $i")
                _messageStateFlow.value = "Executed using ThreadPool: StateFlow $i"
                viewModelScope.launch(ioDispatcher) {
                    _messageSharedFlow.emit("Executed using ThreadPool: SharedFlow $i")
                }
            }, {
                Log.e("MainViewModel", "Error: $it")
            }).also {
                rxJavaCompositeDisposable.add(it)
            }
    }

    override fun onClickExecUsingCoroutines() {
        viewModelScope.launch(ioDispatcher) {
            for (i in 0..10) {
                _messageLiveData.postValue("Executed using Coroutines: LiveData $i")
                _messageStateFlow.value = "Executed using Coroutines: StateFlow $i"
                _messageSharedFlow.emit("Executed using Coroutines: SharedFlow $i")
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        threadPoolExecutor.shutdown()
        rxJavaCompositeDisposable.clear()
    }
}

interface MainEventHandler {
    fun onNavigatePerformanceCompare()
    fun onClickExecUsingThread()
    fun onClickExecUsingThreadPool()
    fun onClickExecUsingRxJava()
    fun onClickExecUsingCoroutines()
}