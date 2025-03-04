/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class com_limo_emumod_bridge_NativeGameBoy */

#ifndef _Included_com_limo_emumod_bridge_NativeGameBoy
#define _Included_com_limo_emumod_bridge_NativeGameBoy
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     com_limo_emumod_bridge_NativeGameBoy
 * Method:    init
 * Signature: (Z)J
 */
JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_init
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     com_limo_emumod_bridge_NativeGameBoy
 * Method:    start
 * Signature: (JLjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_start
  (JNIEnv *, jclass, jlong, jstring, jstring, jstring, jstring);

/*
 * Class:     com_limo_emumod_bridge_NativeGameBoy
 * Method:    stop
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_stop
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_limo_emumod_bridge_NativeGameBoy
 * Method:    updateInput
 * Signature: (JS)V
 */
JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_updateInput
  (JNIEnv *, jclass, jlong, jshort);

/*
 * Class:     com_limo_emumod_bridge_NativeGameBoy
 * Method:    createDisplay
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_createDisplay
  (JNIEnv *, jclass, jlong);

/*
 * Class:     com_limo_emumod_bridge_NativeGameBoy
 * Method:    createAudio
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_createAudio
  (JNIEnv *, jclass, jlong);

#ifdef __cplusplus
}
#endif
#endif
