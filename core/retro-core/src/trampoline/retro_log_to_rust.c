#include <stdarg.h>
#include <stdio.h>

extern void rust_log_trampoline_receiver(unsigned level, const char* msg);

void log_printf_trampoline(unsigned level, const char *fmt, ...) {
    char buffer[1024];

    va_list args;
    va_start(args, fmt);
    vsnprintf(buffer, sizeof(buffer), fmt, args);
    va_end(args);

    rust_log_trampoline_receiver(level, buffer);
}