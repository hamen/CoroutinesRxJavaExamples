package com.ivanmorgillo.coroutinesrxjavaexamples

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.rxSingle

class MainActivity : AppCompatActivity() {

    private val TAG = "CoroutinesVsRx"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fetchFromNetwork()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { Log.d(TAG, "${it.body} - With Rx") },
                { Log.e(TAG, it.message) })


        val job = Job()
        GlobalScope.launch(job) {
            try {
                val response = fetchFromNetwork().await()
                Log.d(TAG, "${response.body} - With Rx to Coroutine adapter")
            } catch (e: Exception) {
                Log.e(TAG, e.message)
            }
            job.join()
        }


        val job2 = Job()
        GlobalScope.launch(job) {
            try {
                val response = loadFromDatabase()
                Log.d(TAG, "${response.body} - With Coroutine")
            } catch (e: Exception) {
                Log.e(TAG, e.message)
            }
            job2.join()
        }

        GlobalScope.rxSingle { loadFromDatabase() }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { Log.d(TAG, "${it.body} - With the Coroutine to Rx adapter") },
                { Log.e(TAG, it.message) })


        val job3 = Job()
        GlobalScope.launch(job3) {
            try {
                val networkResponse = fetchFromNetwork().await()
                val dbResponse = loadFromDatabase()

                Log.d(TAG, "Mixed scenario: half API is Rx, half is Coroutine. We are using them in a Coroutine scenario: ${networkResponse.body} - ${dbResponse.body}")
            } catch (e: Exception) {
                Log.e(TAG, "Something went wrong at some point :P")
            }
            job3.join()
        }

        Single.zip(
            fetchFromNetwork(),
            GlobalScope.rxSingle { loadFromDatabase() },
            BiFunction<SomeObject, SomeObject, Pair<SomeObject, SomeObject>> { networkResponse, dbResponse -> networkResponse to dbResponse })
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { (networkResponse, dbResponse) ->
                    Log.d(TAG, "Mixed scenario: half API is Rx, half is Coroutine. We are using them in a Rx scenario: ${networkResponse.body} - ${dbResponse.body}")
                },
                {
                    Log.e(TAG, "Something went wrong at some point :P")
                }
            )
    }
}


data class SomeObject(val body: String)

fun fetchFromNetwork(): Single<SomeObject> {
    return Single.fromCallable {
        Thread.sleep(2 * 1000)
        SomeObject("I came from da Internet.")
    }
}

suspend fun loadFromDatabase(): SomeObject {
    delay(1 * 1000)
    return SomeObject("I came from da database.")
}
