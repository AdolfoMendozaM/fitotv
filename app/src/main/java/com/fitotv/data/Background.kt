package com.fitotv.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

/**
 * Hilos de baja prioridad para lo que pasa sin que lo pidas (actualizar la lista, bajar la guía).
 * Así la tele le da preferencia a la pantalla y al video, y la interfaz no se traba mientras tanto.
 */
val Background: CoroutineDispatcher = Executors.newFixedThreadPool(2) { r ->
    Thread(r, "fitotv-fondo").apply {
        priority = Thread.MIN_PRIORITY
        isDaemon = true
    }
}.asCoroutineDispatcher()
