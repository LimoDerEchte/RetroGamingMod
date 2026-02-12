//
// Created by limo on 3/13/25.
//

#pragma once

#include <vector>
#include <wels/codec_api.h>
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

class VideoDecoderH264 final : public VideoDecoder {
    ISVCDecoder* decoder;

public:
    VideoDecoderH264(int width, int height);
    ~VideoDecoderH264() override;

    [[nodiscard]] std::vector<int32_t> decodeFrame(const std::vector<uint8_t>& encoded_data) const override;
};

class VideoDecoderAV1 final : public VideoDecoder {
    Dav1dContext* decoder;

public:
    VideoDecoderAV1(int width, int height);
    ~VideoDecoderAV1() override;

    [[nodiscard]] std::vector<int32_t> decodeFrame(const std::vector<uint8_t>& encoded_data) const override;
};

class VideoDecoderWebP final : public VideoDecoder {
public:
    VideoDecoderWebP(int width, int height);

    [[nodiscard]] std::vector<int32_t> decodeFrame(const std::vector<uint8_t>& encoded_data) const override;
};
