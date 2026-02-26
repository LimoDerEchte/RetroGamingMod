use std::process::Command;
use retro_shared::shared::shared_memory::SharedMemory;
use crate::codec::audio_decoder::AudioDecoder;
use crate::codec::video_decoder::VideoDecoder;

pub struct GenericConsole {
    video_decoder: Option<Box<dyn VideoDecoder>>,
    audio_encoder: Option<Box<dyn AudioDecoder>>,

    shared_data: SharedMemory,
    core_process: Option<Command>,
}

// TODO: GenericConsole::GenericConsole(const int width, const int height, const int sampleRate, const int codec, const jUUID* uuid, const jUUID* consoleId)
// TODO: void GenericConsole::load(const char *retroCore, const char *core, const char *rom, const char *save) {
// TODO: void GenericConsole::dispose() {
// TODO: std::vector<uint8_t> GenericConsole::createFrame() {
// TODO: std::vector<uint8_t> GenericConsole::createClip() {
// TODO: void GenericConsole::input(const int port, const int16_t input) const {
// TODO: void GenericConsoleRegistry::registerConsole(GenericConsole *console) {
// TODO: void GenericConsoleRegistry::unregisterConsole(GenericConsole *console) {
// TODO: void GenericConsoleRegistry::withConsoles(const bool writing, const std::function<void(GenericConsole*)>& func) {
// TODO: void GenericConsoleRegistry::withConsole(const bool writing, const jUUID *uuid, const std::function<void(GenericConsole*)> &func) {
