
#pragma once

#ifdef _WIN32
#    ifdef LIBRARY_EXPORTS
#        define LIBRARY_API __declspec(dllexport)
#    else
#        define LIBRARY_API __declspec(dllimport)
#    endif
#else
#    define LIBRARY_API
#endif

#include <functional>

extern "C" LIBRARY_API bool coreLoad(const char* core);
extern "C" LIBRARY_API bool romLoad(const char* rom);
extern "C" LIBRARY_API void updateInput(short input);
extern "C" LIBRARY_API void setVideoCallback(const std::function<void(const int*, unsigned, unsigned, size_t)> &videoCallback);
extern "C" LIBRARY_API void start();
