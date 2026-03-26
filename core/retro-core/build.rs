
fn main() {
    cc::Build::new()
        .file("src/trampoline/retro_log_to_rust.c")
        .compile("trampolines");
}
