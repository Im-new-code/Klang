#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL
Java_dev_klang_portal_audio_NativeVisualizerBridge_nativeStatus(JNIEnv *env, jobject /* thiz */) {
    return env->NewStringUTF("Optional native visualizer is not bundled in this build");
}
