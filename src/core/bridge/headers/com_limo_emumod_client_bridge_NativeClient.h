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
 * Signature: (Ljava/lang/String;ILjava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_limo_emumod_client_bridge_NativeClient_connect
  (JNIEnv *, jclass, jstring, jint, jstring);

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
 * Method:    registerScreen
 * Signature: (JJII)J
 */
JNIEXPORT jlong JNICALL Java_com_limo_emumod_client_bridge_NativeClient_registerScreen
  (JNIEnv *, jclass, jlong, jlong, jint, jint);

/*
 * Class:     com_limo_emumod_client_bridge_NativeClient
 * Method:    unregisterScreen
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_limo_emumod_client_bridge_NativeClient_unregisterScreen
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     com_limo_emumod_client_bridge_NativeClient
 * Method:    registerAudio
 * Signature: (JJI)J
 */
JNIEXPORT jlong JNICALL Java_com_limo_emumod_client_bridge_NativeClient_registerAudio
  (JNIEnv *, jclass, jlong, jlong, jint);

/*
 * Class:     com_limo_emumod_client_bridge_NativeClient
 * Method:    unregisterAudio
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_limo_emumod_client_bridge_NativeClient_unregisterAudio
  (JNIEnv *, jclass, jlong, jlong);

/*
 * Class:     com_limo_emumod_client_bridge_NativeClient
 * Method:    sendControlUpdate
 * Signature: (JJIS)V
 */
JNIEXPORT void JNICALL Java_com_limo_emumod_client_bridge_NativeClient_sendControlUpdate
  (JNIEnv *, jclass, jlong, jlong, jint, jshort);

#ifdef __cplusplus
}
#endif
#endif
