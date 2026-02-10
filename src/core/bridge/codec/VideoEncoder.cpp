//
// Created by limo on 3/13/25.
//

#include "VideoEncoder.hpp"

#include <libyuv.h>
#include <webp/encode.h>

VideoEncoder::VideoEncoder(const int width, const int height) : width(width), height(height) {
}

std::vector<uint8_t> VideoEncoder::encodeFrameRGB565(const std::vector<int16_t> &frame) const {
    return {};
}

int VideoEncoder::getWidth() const {
    return width;
}

int VideoEncoder::getHeight() const {
    return height;
}

VideoEncoderH264::VideoEncoderH264(const int width, const int height) : VideoEncoder(width, height), encoder(nullptr) {
    WelsCreateSVCEncoder(&encoder);

    SEncParamExt param = {};
    encoder->GetDefaultParams(&param);

    param.iUsageType = SCREEN_CONTENT_REAL_TIME;
    param.iPicWidth = width;
    param.iPicHeight = height;
    param.iTargetBitrate = 1000000;
    param.iRCMode = RC_QUALITY_MODE;
    param.fMaxFrameRate = 60.0f;
    param.uiIntraPeriod = 60;
    param.bEnableBackgroundDetection = false;
    param.bEnableAdaptiveQuant = false;

    param.sSpatialLayers[0].uiProfileIdc = PRO_BASELINE;
    param.sSpatialLayers[0].uiLevelIdc   = LEVEL_3_1;
    param.sSpatialLayers[0].iVideoWidth  = width;
    param.sSpatialLayers[0].iVideoHeight = height;
    param.sSpatialLayers[0].fFrameRate   = 60.0f;
    param.sSpatialLayers[0].iSpatialBitrate = 1000000;

    encoder->InitializeExt(&param);
}

VideoEncoderH264::~VideoEncoderH264() {
    encoder->Uninitialize();
    WelsDestroySVCEncoder(encoder);
}

std::vector<uint8_t> VideoEncoderH264::encodeFrameRGB565(const std::vector<int16_t> &frame) const {
    std::vector<uint8_t> y(width*height), u(width*height/4), v(width*height/4);
    libyuv::RGB565ToI420(reinterpret_cast<const uint8_t*>(frame.data()), width*2,
                       y.data(), width,
                       u.data(), width/2,
                       v.data(), width/2,
                       width, height);

    SSourcePicture pic = {};
    pic.iPicWidth = width; pic.iPicHeight = height;
    pic.iColorFormat = videoFormatI420;
    pic.iStride[0]=width; pic.iStride[1]=width/2; pic.iStride[2]=width/2;
    pic.pData[0]=y.data(); pic.pData[1]=u.data(); pic.pData[2]=v.data();

    SFrameBSInfo info = {};
    encoder->EncodeFrame(&pic, &info);

    std::vector<uint8_t> output;
    for (int i = 0; i < info.iLayerNum; ++i) {
        const SLayerBSInfo &layer = info.sLayerInfo[i];
        unsigned char* bufPtr = layer.pBsBuf;
        for (int j = 0; j < layer.iNalCount; ++j) {
            output.insert(output.end(), bufPtr, bufPtr + layer.pNalLengthInByte[j]);
            bufPtr += layer.pNalLengthInByte[j];
        }
    }
    return output;
}

VideoEncoderWebP::VideoEncoderWebP(const int width, const int height) : VideoEncoder(width, height) {
}

std::vector<uint8_t> VideoEncoderWebP::encodeFrameRGB565(const std::vector<int16_t> &frame) const {
    if (frame.empty()) return {};

    std::vector<uint8_t> argb(width * height * 4);
    libyuv::RGB565ToARGB(
        reinterpret_cast<const uint8_t*>(frame.data()),width * 2,
        argb.data(), width * 4,
        width, height
    );

    uint8_t* encoded = nullptr;
    const size_t encodedSize = WebPEncodeLosslessRGBA(argb.data(), width, height, width * 4, &encoded);

    if (encoded == nullptr || encodedSize == 0)
        return {};

    std::vector output(encoded, encoded + encodedSize);
    WebPFree(encoded);
    return output;
}
