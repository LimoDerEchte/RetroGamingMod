/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_limo_emumod_client_bridge_NativeClient */

#ifndef _Included_com_limo_emumod_client_bridge_NativeClient
#define _Included_com_limo_emumod_client_bridge_NativeClient
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_limo_emumod_client_bridge_NativeClient
 * Method:    connect
 * Signature: (ILjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_limo_emumod_client_bridge_NativeClient_connect
  (JNIEnv *, jclass, jint, jstring);

/*
 * Class:     com_limo_emumod_client_bridge_NativeClient
 * Method:    disconnect
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_limo_emumod_client_bridge_NativeClient_disconnect
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_limo_emumod_client_bridge_NativeClient
 * Method:    isAuthenticated
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_com_limo_emumod_client_bridge_NativeClient_isAuthenticated
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_limo_emumod_client_bridge_NativeClient
 * Method:    poll
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_limo_emumod_client_bridge_NativeClient_poll
  (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif
