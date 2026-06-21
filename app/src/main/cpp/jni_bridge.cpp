#include <jni.h>
#include <cstring>
#include "native_processor.h"
#include "audio_common.h"

static jclass bridgeClass = nullptr;
static jmethodID onErrorMethod = nullptr;
static JavaVM* jvm = nullptr;

// JNI_OnLoad – cache class and method IDs
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    jvm = vm;
    JNIEnv* env = nullptr;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;

    jclass localBridge = env->FindClass("com/pirate/audio8d/NativeBridge");
    if (!localBridge) return JNI_ERR;
    bridgeClass = static_cast<jclass>(env->NewGlobalRef(localBridge));
    env->DeleteLocalRef(localBridge);

    // Cache method ID for onError (if needed)
    // onErrorMethod = env->GetStaticMethodID(bridgeClass, "onNativeError", "(Ljava/lang/String;)V");
    // Not used in this example, but ready.

    return JNI_VERSION_1_6;
}

// Helper to get processor instance
NativeProcessor* getProcessor() {
    return g_processor;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_pirate_audio8d_NativeBridge_initEngine(JNIEnv*, jclass, jint sampleRate, jint framesPerBurst) {
    if (g_processor) {
        g_processor->destroy();
        delete g_processor;
    }
    g_processor = new NativeProcessor();
    bool ok = g_processor->init(sampleRate, framesPerBurst);
    if (!ok) {
        delete g_processor;
        g_processor = nullptr;
    }
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_pirate_audio8d_NativeBridge_destroyEngine(JNIEnv*, jclass) {
    if (g_processor) {
        g_processor->destroy();
        delete g_processor;
        g_processor = nullptr;
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_pirate_audio8d_NativeBridge_setOrbitSpeed(JNIEnv*, jclass, jfloat speed) {
    if (g_processor) g_processor->setOrbitSpeed(speed);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_pirate_audio8d_NativeBridge_setDepth(JNIEnv*, jclass, jfloat depth) {
    if (g_processor) g_processor->setDepth(depth);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_pirate_audio8d_NativeBridge_setReverbMix(JNIEnv*, jclass, jfloat mix) {
    if (g_processor) g_processor->setReverbMix(mix);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_pirate_audio8d_NativeBridge_setDelayMix(JNIEnv*, jclass, jfloat mix) {
    if (g_processor) g_processor->setDelayMix(mix);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_pirate_audio8d_NativeBridge_setBassBoost(JNIEnv*, jclass, jfloat db) {
    if (g_processor) g_processor->setBassBoost(db);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_pirate_audio8d_NativeBridge_startPlayback(JNIEnv*, jclass) {
    return g_processor ? g_processor->startPlayback() : JNI_FALSE;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_pirate_audio8d_NativeBridge_stopPlayback(JNIEnv*, jclass) {
    if (g_processor) g_processor->stopPlayback();
}

extern "C"
JNIEXPORT void JNICALL
Java_com_pirate_audio8d_NativeBridge_processCapture(JNIEnv* env, jclass, jobject byteBuffer, jint numFrames) {
    if (!g_processor) return;
    void* direct = env->GetDirectBufferAddress(byteBuffer);
    if (!direct) return;
    const int16_t* shorts = static_cast<const int16_t*>(direct);
    g_processor->processCapture(shorts, numFrames);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_pirate_audio8d_NativeBridge_generateTestTone(JNIEnv*, jclass, jint numFrames) {
    if (g_processor) g_processor->generateTestTone(numFrames);
}