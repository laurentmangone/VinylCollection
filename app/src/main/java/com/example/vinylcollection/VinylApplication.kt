package com.example.vinylcollection

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Classe Application pour initialiser les composants globaux
 * Configure Coil avec OkHttp pour charger les images Discogs correctement
 */
class VinylApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        Log.d("VinylApp", "Application démarrée - Coil configuré pour charger les images Discogs")
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient {
                OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor { chain ->
                        val originalRequest = chain.request()
                        val requestWithUserAgent = originalRequest.newBuilder()
                            .header("User-Agent", "VinylCollection/1.0 (Android)")
                            .build()
                        val response = chain.proceed(requestWithUserAgent)
                        Log.d("Coil", "Image request: ${requestWithUserAgent.url}")
                        Log.d("Coil", "Response code: ${response.code}")
                        response
                    }
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false) // Ignore les headers de cache pour forcer le chargement
            .build()
    }
}

