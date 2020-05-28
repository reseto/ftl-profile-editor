#include <jni.h>
#include "rand.h"
#include <stdlib.h>

JNIEXPORT jint JNICALL Java_net_blerf_ftl_parser_random_NativeRandomJNI_native_1rand
  (JNIEnv *e, jobject o)
{
    return rand();
}

JNIEXPORT void JNICALL Java_net_blerf_ftl_parser_random_NativeRandomJNI_native_1srand
  (JNIEnv *e, jobject o, jint seed)
{
    srand(seed);
}

