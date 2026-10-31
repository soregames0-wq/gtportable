// Ponte JNI entre o app Android e o núcleo recompilado.
//
// O núcleo (pasta core/) precisa exportar estas 3 funções C:
//   int  gt_recomp_run(ANativeWindow* win, int iso_fd);   // bloqueante: roda o jogo
//   void gt_recomp_request_stop();
//   void gt_recomp_set_input(unsigned buttons, unsigned char ax, unsigned char ay);
//
// Sem o núcleo, um modo de teste pinta a tela e reage aos botões (valida UI/controles).
#include <jni.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <atomic>
#include <thread>
#include <chrono>

#define TAG "GTBridge"
#define LOG(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

#ifdef GT_RECOMP_CORE
extern "C" {
int  gt_recomp_run(ANativeWindow* win, int iso_fd);
void gt_recomp_request_stop();
void gt_recomp_set_input(unsigned buttons, unsigned char ax, unsigned char ay);
}
#endif

static std::atomic<unsigned> g_buttons{0};
static std::atomic<int> g_ax{128}, g_ay{128};
static std::atomic<bool> g_run{false};
static std::thread g_thread;
static ANativeWindow* g_win = nullptr;

#ifndef GT_RECOMP_CORE
static void stub_loop() {
    ANativeWindow_setBuffersGeometry(g_win, 0, 0, WINDOW_FORMAT_RGBA_8888);
    while (g_run) {
        ANativeWindow_Buffer buf;
        if (ANativeWindow_lock(g_win, &buf, nullptr) == 0) {
            unsigned b = g_buttons.load();
            unsigned color = b ? 0xFF5A3A1A : 0xFF301A0A;       // ABGR: azul muda ao apertar
            for (int y = 0; y < buf.height; y++) {
                unsigned* row = (unsigned*)buf.bits + y * buf.stride;
                for (int x = 0; x < buf.width; x++) row[x] = color;
            }
            ANativeWindow_unlockAndPost(g_win);
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(16));
    }
}
#endif

extern "C" {

JNIEXPORT void JNICALL Java_com_gtportable_Native_start(JNIEnv* env, jobject, jobject surface, jint isoFd) {
    if (g_run) return;
    g_win = ANativeWindow_fromSurface(env, surface);
    g_run = true;
    int fd = isoFd;
    g_thread = std::thread([fd] {
#ifdef GT_RECOMP_CORE
        LOG("iniciando núcleo recompilado");
        gt_recomp_run(g_win, fd);
#else
        LOG("núcleo não ligado: modo de teste (fd=%d)", fd);
        stub_loop();
#endif
    });
}

JNIEXPORT void JNICALL Java_com_gtportable_Native_stop(JNIEnv*, jobject) {
    if (!g_run) return;
    g_run = false;
#ifdef GT_RECOMP_CORE
    gt_recomp_request_stop();
#endif
    if (g_thread.joinable()) g_thread.join();
    if (g_win) { ANativeWindow_release(g_win); g_win = nullptr; }
}

JNIEXPORT void JNICALL Java_com_gtportable_Native_input(JNIEnv*, jobject, jint buttons, jint ax, jint ay) {
    g_buttons = (unsigned)buttons; g_ax = ax; g_ay = ay;
#ifdef GT_RECOMP_CORE
    gt_recomp_set_input((unsigned)buttons, (unsigned char)ax, (unsigned char)ay);
#endif
}

}
