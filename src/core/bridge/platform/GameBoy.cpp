//
// Created by limo on 2/21/25.
//

#include "GameBoy.hpp"

#include <iostream>
#include <headers/com_limo_emumod_bridge_NativeGameBoy.h>
#include <jni.h>

#include "util/util.hpp"

namespace bip = boost::interprocess;
namespace bp  = boost::process;

JNIEXPORT jlong JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_init(JNIEnv *, jclass) {
    return reinterpret_cast<jlong>(new GameBoy());
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_start(JNIEnv *env, jclass, const jlong ptr, const jstring rom, jstring) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->allocate(env->GetStringUTFChars(rom, nullptr));
    gameboy->start();
}

JNIEXPORT void JNICALL Java_com_limo_emumod_bridge_NativeGameBoy_stop(JNIEnv *, jclass, const jlong ptr) {
    const auto gameboy = reinterpret_cast<GameBoy*>(ptr);
    gameboy->dispose();
}

GameBoy::GameBoy() {
    GenerateID(id);
}

void GameBoy::allocate(const char *rom) {
    bip::shared_memory_object::remove(id);
    segment = new bip::managed_shared_memory(bip::create_only, id, sizeof(GameBoyShared));

    shared = segment->construct<GameBoyShared>("SharedData")();
    shared->rom = rom;
}

void GameBoy::start() {
    std::cout << "[RetroGamingCore] Starting up new bridge instance" << id << std::endl;
    child = new boost::process::child(PATH_TO_LIB, id);
}

void GameBoy::dispose() const {
    std::cout << "[RetroGamingCore] Disposing bridge instance" << id << std::endl;
// ReSharper disable once CppDFANullDereference
    child->terminate();
// ReSharper disable once CppDFANullDereference
    child->wait();
// ReSharper disable once CppDFANullDereference
    segment->destroy<GameBoyShared>("SharedData");
    bip::shared_memory_object::remove(id);
}

