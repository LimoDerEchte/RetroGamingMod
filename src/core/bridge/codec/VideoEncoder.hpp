
#pragma once

#include <vector>
#include <wels/codec_api.h>
#include <svt-av1/EbSvtAv1Enc.h>

class VideoEncoder {
protected:
    const int width, height;

public:
    VideoEncoder(int width, int height);
    virtual ~VideoEncoder() = default;

    [[nodiscard]] virtual std::vector<uint8_t> encodeFrameRGB565(const std::vector<int16_t>& frame) const;

    [[nodiscard]] int getWidth() const;
    [[nodiscard]] int getHeight() const;
};

class VideoEncoderH264 final : public VideoEncoder {
    ISVCEncoder* encoder;

public:
    VideoEncoderH264(int width, int height);
    ~VideoEncoderH264() override;

    [[nodiscard]] std::vector<uint8_t> encodeFrameRGB565(const std::vector<int16_t>& frame) const override;
};

class VideoEncoderAV1 final : public VideoEncoder {
    EbComponentType* encoder;

public:
    VideoEncoderAV1(int width, int height);
    ~VideoEncoderAV1() override;

    [[nodiscard]] std::vector<uint8_t> encodeFrameRGB565(const std::vector<int16_t>& frame) const override;
};

class VideoEncoderWebP final : public VideoEncoder {
public:
    VideoEncoderWebP(int width, int height);

    [[nodiscard]] std::vector<uint8_t> encodeFrameRGB565(const std::vector<int16_t>& frame) const override;
};
