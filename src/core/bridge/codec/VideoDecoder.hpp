//
// Created by limo on 3/13/25.
//

#pragma once

#include <vector>
#include <dav1d/dav1d.h>

class VideoDecoder {
protected:
    const int width, height;

public:
    VideoDecoder(int width, int height);
    virtual ~VideoDecoder() = default;

    [[nodiscard]] virtual std::vector<int32_t> decodeFrame(const std::vector<uint8_t>& encoded_data) const;

    [[nodiscard]] int getWidth() const;
    [[nodiscard]] int getHeight() const;
};

class VideoDecoderAV1 final : public VideoDecoder {
    Dav1dContext* decoder;

public:
    VideoDecoderAV1(int width, int height);
    ~VideoDecoderAV1() override;

    [[nodiscard]] std::vector<int32_t> decodeFrame(const std::vector<uint8_t>& encoded_data) const override;
};
