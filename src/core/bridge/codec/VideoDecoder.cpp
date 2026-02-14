
#include "VideoDecoder.hpp"

#include <libyuv.h>
#include <climits>
#include <ostream>
#include <dav1d/dav1d.h>

VideoDecoder::VideoDecoder(const int width, const int height) : width(width), height(height) {
}

std::vector<int32_t> VideoDecoder::decodeFrame(const std::vector<uint8_t> &encoded_data) const {
    return {};
}

int VideoDecoder::getWidth() const {
    return width;
}

int VideoDecoder::getHeight() const {
    return height;
}

VideoDecoderAV1::VideoDecoderAV1(const int width, const int height) : VideoDecoder(width, height), decoder(nullptr) {
    Dav1dSettings settings;
    dav1d_default_settings(&settings);

    settings.n_threads = 4;
    settings.max_frame_delay = 1;

    if (DAV1D_ERR(dav1d_open(&decoder, &settings))) {
        return;
    }
}

VideoDecoderAV1::~VideoDecoderAV1() {
    dav1d_close(&decoder);
}

std::vector<int32_t> VideoDecoderAV1::decodeFrame(const std::vector<uint8_t> &encoded_data) const {
    if (!decoder) return {};

    Dav1dData data;
    if (DAV1D_ERR(dav1d_data_wrap(&data, encoded_data.data(), encoded_data.size(), [](const uint8_t *buf, void *cookie){}, nullptr))) {
        return {};
    }

    if (DAV1D_ERR(dav1d_send_data(decoder, &data))) {
        dav1d_data_unref(&data);
        return {};
    }

    Dav1dPicture pic;
    if (DAV1D_ERR(dav1d_get_picture(decoder, &pic) < 0)) {
        return {};
    }

    const int frameWidth = pic.p.w;
    const int frameHeight = pic.p.h;

    std::vector<int32_t> argb(frameWidth * frameHeight);

    libyuv::I420ToABGR(static_cast<const uint8_t*>(pic.data[0]), static_cast<int>(pic.stride[0]),
                       static_cast<const uint8_t*>(pic.data[1]), static_cast<int>(pic.stride[1]),
                       static_cast<const uint8_t*>(pic.data[2]), static_cast<int>(pic.stride[1]),
                       reinterpret_cast<uint8_t*>(argb.data()), frameWidth*4,
                       frameWidth, frameHeight);

    dav1d_picture_unref(&pic);
    return argb;
}
