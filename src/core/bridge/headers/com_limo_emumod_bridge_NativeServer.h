/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_limo_emumod_bridge_NativeServer */

#ifndef _Included_com_limo_emumod_bridge_NativeServer
#define _Included_com_limo_emumod_bridge_NativeServer
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_limo_emumod_bridge_NativeServer
 * Method:    startServer
 * Signature: (I)J
 */
JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeServer_startServer
  (JNIEnv *, jclass, jint);

/*
 * Class:     com_limo_emumod_bridge_NativeServer
 * Method:    stopServer
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeServer_stopServer
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_limo_emumod_bridge_NativeServer
 * Method:    poll
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeServer_poll
  (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif
