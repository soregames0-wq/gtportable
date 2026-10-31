package com.gtportable

import android.view.Surface

/** Ponte JNI para o núcleo recompilado (libgtbridge.so). */
object Native {
    init { System.loadLibrary("gtbridge") }
    external fun start(surface: Surface, isoFd: Int)
    external fun stop()
    /** buttons = máscara PSP; ax/ay = analógico 0..255 (128 = centro). */
    external fun input(buttons: Int, ax: Int, ay: Int)
}
