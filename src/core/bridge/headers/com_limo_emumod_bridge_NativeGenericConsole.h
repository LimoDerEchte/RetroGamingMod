/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_limo_emumod_bridge_NativeGenericConsole */

#ifndef _Included_com_limo_emumod_bridge_NativeGenericConsole
#define _Included_com_limo_emumod_bridge_NativeGenericConsole
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_limo_emumod_bridge_NativeGenericConsole
 * Method:    init
 * Signature: (JIII)J
 */
JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_init
  (JNIEnv *, jclass, jlong, jint, jint, jint);

/*
 * Class:     com_limo_emumod_bridge_NativeGenericConsole
 * Method:    start
 * Signature: (JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_start
  (JNIEnv *, jclass, jlong, jstring, jstring, jstring, jstring);

/*
 * Class:     com_limo_emumod_bridge_NativeGenericConsole
 * Method:    stop
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_stop
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_limo_emumod_bridge_NativeGenericConsole
 * Method:    getWidth
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_getWidth
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_limo_emumod_bridge_NativeGenericConsole
 * Method:    getHeight
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_getHeight
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_limo_emumod_bridge_NativeGenericConsole
 * Method:    getSampleRate
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_com_limo_emumod_bridge_NativeGenericConsole_getSampleRate
  (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif
