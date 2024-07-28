package com.creative.androidconcurrencyexamples

import android.util.Log
import androidx.lifecycle.ViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject

/**
 * Created by dan on 28/7/24
 *
 * Copyright © 2024 1010 Creative. All rights reserved.
 */

class BackpressureViewModel : ViewModel() {

    private val DummyDataSize = 4096 * 16

    private val compositeDisposable = CompositeDisposable()

    private val subject: PublishSubject<LongArray> = PublishSubject.create()
    private val observable: Observable<LongArray> = Observable.create {
        var i = 0L
        while (true) {
            i += 1
            it.onNext(LongArray(DummyDataSize) { i }) // simulate large data
            Thread.sleep(10) // emit every 10 ms
        }
    }

    fun onRxJavaSubjectBackpressure() {
        val disposable = subject.subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .subscribe({
                Thread.sleep(100) // simulate computation
                Log.d("BackpressureViewModel", "onRxJavaSubjectBackpressure: ${it[0]}")
            }, {
                Log.d("BackpressureViewModel", "onRxJavaSubjectBackpressure: Error: $it")
            })
        compositeDisposable.add(disposable)

        Thread {
            var i = 0L
            while (true) {
                i += 1
                subject.onNext(LongArray(DummyDataSize) { i }) // simulate large data
                Thread.sleep(10) // emit every 1 ms
            }
        }.start()
    }

    fun onRxJavaObservableBackpressure() {
        val disposable = observable.subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .subscribe({
                Thread.sleep(100) // simulate computation
                Log.d("BackpressureViewModel", "onRxJavaObservableBackpressure: ${it[0]}")
            }, {
                Log.d("BackpressureViewModel", "onRxJavaObservableBackpressure: Error: $it")
            })
        compositeDisposable.add(disposable)
    }

    fun onRxJavaFlowableBackpressure(backpressureStrategy: BackpressureStrategy) {
        val disposable = Flowable.create<LongArray>({
            var i = 0L
            while (true) {
                i += 1
                it.onNext(LongArray(DummyDataSize) {
                    i
                }) // simulate large data
                Thread.sleep(10) // emit every 10 ms
            }
        }, backpressureStrategy)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .subscribe({
                Thread.sleep(100) // simulate computation
                Log.d("BackpressureViewModel", "onRxJavaFlowableBackpressure: ${it[0]}")
            }, {
                Log.d("BackpressureViewModel", "onRxJavaFlowableBackpressure: Error: $it")
            })
        compositeDisposable.add(disposable)
    }

    fun onHandleBackpressureSubjectByFlowable() {
        val disposable = subject
            .toFlowable(BackpressureStrategy.DROP)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .subscribe({
                Thread.sleep(100) // simulate computation
                Log.d("BackpressureViewModel", "onRxJavaSubjectBackpressure: ${it[0]}")
            }, {
                Log.d("BackpressureViewModel", "onRxJavaSubjectBackpressure: Error: $it")
            })
        compositeDisposable.add(disposable)

        Thread {
            var i = 0L
            while (true) {
                i += 1
                subject.onNext(LongArray(DummyDataSize) { i }) // simulate large data
                Thread.sleep(10) // emit every 1 ms
            }
        }.start()
    }

    fun onHandleBackpressureObservableByFlowable() {
        val disposable = observable
            .toFlowable(BackpressureStrategy.DROP)
            .subscribeOn(Schedulers.io(), false)
            .observeOn(Schedulers.computation())
            .subscribe({
                Thread.sleep(100) // simulate computation
                Log.d("BackpressureViewModel", "onRxJavaObservableBackpressure: ${it[0]}")
            }, {
                Log.d("BackpressureViewModel", "onRxJavaObservableBackpressure: Error: $it")
            })
        compositeDisposable.add(disposable)
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.dispose()
    }
}