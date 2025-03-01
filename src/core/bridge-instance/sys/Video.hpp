
#pragma once
#include <vector>
#include <functional>
#include <cstddef>

class Video {
public:
    using FrameFinishedCallback = std::function<void(const std::vector<int>& pixels, unsigned width, unsigned height)>;

    Video();
    ~Video();

    void setFrameFinishedCallback(const FrameFinishedCallback &callback);
    void videoRefresh(const void* data, unsigned width, unsigned height, size_t pitch);

    const std::vector<int>& getPixels() const;

private:
    std::vector<int> m_pixels;             // Storage for pixel data.
    FrameFinishedCallback m_callback;      // Callback to be invoked after a frame is processed.
};