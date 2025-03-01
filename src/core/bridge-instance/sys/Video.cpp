
#include "Video.hpp"
#include <cstring>

Video::Video() : m_callback(nullptr) {}

Video::~Video() {}

void Video::setFrameFinishedCallback(const FrameFinishedCallback &callback) {
    m_callback = callback;
}

void Video::videoRefresh(const void* data, const unsigned width, const unsigned height, const size_t pitch) {
    if (!data)
        return;
    m_pixels.resize(width * height);
    const unsigned intsPerRow = pitch / sizeof(int);
    const auto src = static_cast<const int*>(data);
    for (unsigned y = 0; y < height; ++y) {
        const int* srcRow = src + y * intsPerRow;
        int* dstRow = &m_pixels[y * width];
        std::memcpy(dstRow, srcRow, width * sizeof(int));
    }
    if (m_callback) {
        m_callback(m_pixels, width, height);
    }
}

const std::vector<int>& Video::getPixels() const {
    return m_pixels;
}
