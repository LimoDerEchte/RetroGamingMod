
#pragma once

#include <vector>
#include <EbSvtAv1Enc.h>

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

class VideoEncoderAV1 final : public VideoEncoder {
    EbComponentType* encoder;

public:
    VideoEncoderAV1(int width, int height);
    ~VideoEncoderAV1() override;

    [[nodiscard]] std::vector<uint8_t> encodeFrameRGB565(const std::vector<int16_t>& frame) const override;
};
