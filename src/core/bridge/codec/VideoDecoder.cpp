//
// Created by limo on 3/13/25.
//

#include "VideoDecoder.hpp"

#include <libyuv.h>
#include <climits>
#include <ostream>
#include <webp/decode.h>

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

VideoDecoderH264::VideoDecoderH264(const int width, const int height) : VideoDecoder(width, height), decoder(nullptr) {
    SDecodingParam param = {};
    param.sVideoProperty.eVideoBsType = VIDEO_BITSTREAM_DEFAULT;

    param.uiTargetDqLayer = UCHAR_MAX;
    param.eEcActiveIdc = ERROR_CON_SLICE_COPY;

    WelsCreateDecoder(&decoder);
    decoder->Initialize(&param);
}

VideoDecoderH264::~VideoDecoderH264() {
    decoder->Uninitialize();
    WelsDestroyDecoder(decoder);
}

std::vector<int32_t> VideoDecoderH264::decodeFrame(const std::vector<uint8_t> &encoded_data) const {
    if (!decoder) return {};

    std::vector<uint8_t> y(width * height);
    std::vector<uint8_t> u(width * height / 4);
    std::vector<uint8_t> v(width * height / 4);

    unsigned char* ppDst[3] = { y.data(), u.data(), v.data() };

    SBufferInfo bufInfo = {};
    if (const int ret = decoder->DecodeFrameNoDelay(encoded_data.data(), static_cast<int>(encoded_data.size()),
            ppDst, &bufInfo); ret != 0 || bufInfo.iBufferStatus != 1)
        return {};

    const int frameWidth = bufInfo.UsrData.sSystemBuffer.iWidth;
    const int frameHeight = bufInfo.UsrData.sSystemBuffer.iHeight;

    std::vector<int32_t> argb(frameWidth * frameHeight);
    libyuv::I420ToABGR(bufInfo.pDst[0], bufInfo.UsrData.sSystemBuffer.iStride[0],
                       bufInfo.pDst[1], bufInfo.UsrData.sSystemBuffer.iStride[1],
                       bufInfo.pDst[2], bufInfo.UsrData.sSystemBuffer.iStride[1],
                       reinterpret_cast<uint8_t *>(argb.data()), frameWidth*4,
                       frameWidth, frameHeight);
    return argb;
}

VideoDecoderWebP::VideoDecoderWebP(const int width, const int height) : VideoDecoder(width, height) {
}

std::vector<int32_t> VideoDecoderWebP::decodeFrame(const std::vector<uint8_t> &encoded_data) const {
    if (encoded_data.empty())
        return {};

    int width = 0, height = 0;
    uint8_t* rgba = WebPDecodeRGBA(encoded_data.data(), encoded_data.size(), &width, &height);
    if (!rgba)
        return {};

    std::vector<int32_t> output(width * height);
    libyuv::ARGBToABGR(rgba, width * 4,
                        reinterpret_cast<uint8_t *>(output.data()), width * 4,
                        width, height);
    WebPFree(rgba);
    return output;
}
