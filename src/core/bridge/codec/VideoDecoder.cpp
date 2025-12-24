//
// Created by limo on 3/13/25.
//

#include "VideoDecoder.hpp"

#include <algorithm>
#include <climits>
#include <iostream>
#include <ostream>

VideoDecoder::VideoDecoder(const int width, const int height) : width(width), height(height), decoder(nullptr) {
    SDecodingParam param = {};
    param.sVideoProperty.eVideoBsType = VIDEO_BITSTREAM_DEFAULT;

    param.uiTargetDqLayer = UCHAR_MAX;
    param.eEcActiveIdc = ERROR_CON_SLICE_COPY;

    WelsCreateDecoder(&decoder);
    decoder->Initialize(&param);
}

VideoDecoder::~VideoDecoder() {
    decoder->Uninitialize();
    WelsDestroyDecoder(decoder);
}

std::vector<int32_t> VideoDecoder::decodeFrame(const std::vector<uint8_t> &encoded_data) const {
    if (!decoder) return {};
    std::cout << "Decoding " << encoded_data.size() << " bytes" << std::endl;

    SBufferInfo bufInfo = {};
    if (const int ret = decoder->DecodeFrameNoDelay(encoded_data.data(), static_cast<int>(encoded_data.size()),
            nullptr, &bufInfo); ret != 0 || bufInfo.iBufferStatus != 1)
        return {};

    const int frameWidth  = bufInfo.UsrData.sSystemBuffer.iWidth;
    const int frameHeight = bufInfo.UsrData.sSystemBuffer.iHeight;

    const uint8_t* yPlane  = bufInfo.pDst[0];
    const uint8_t* uvPlane = bufInfo.pDst[1];

    const int yStride  = bufInfo.UsrData.sSystemBuffer.iStride[0];
    const int uvStride = bufInfo.UsrData.sSystemBuffer.iStride[1];

    std::vector<int32_t> rgba(frameWidth * frameHeight);
    for (int j = 0; j < frameHeight; ++j) {
        for (int i = 0; i < frameWidth; ++i) {
            const int yIdx = j * yStride + i;
            const int uvIdx = (j / 2) * uvStride + (i / 2) * 2; // *2 because U+V

            const uint8_t Y = yPlane[yIdx];
            const uint8_t U = uvPlane[uvIdx + 0];
            const uint8_t V = uvPlane[uvIdx + 1];

            // convert YUV->RGB
            const int C = Y - 16;
            const int D = U - 128;
            const int E = V - 128;

            int R = (298 * C + 409 * E + 128) >> 8;
            int G = (298 * C - 100 * D - 208 * E + 128) >> 8;
            int B = (298 * C + 516 * D + 128) >> 8;

            R = std::clamp(R, 0, 255);
            G = std::clamp(G, 0, 255);
            B = std::clamp(B, 0, 255);

            rgba[j * frameWidth + i] = (R << 24) | (G << 16) | (B << 8) | 0xFF;
        }
    }
    return rgba;
}

int VideoDecoder::getWidth() const {
    return width;
}

int VideoDecoder::getHeight() const {
    return height;
}
