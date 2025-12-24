//
// Created by limo on 3/13/25.
//

#include "VideoEncoder.hpp"

#include <libyuv.h>

VideoEncoderInt16::VideoEncoderInt16(const int width, const int height) : width(width), height(height), encoder(nullptr) {
    WelsCreateSVCEncoder(&encoder);

    SEncParamExt param = {};
    encoder->GetDefaultParams(&param);

    param.iUsageType = SCREEN_CONTENT_REAL_TIME;
    param.iPicWidth = width;
    param.iPicHeight = height;
    param.iTargetBitrate = TARGET_BITRATE;
    param.iRCMode = RC_BITRATE_MODE;
    param.fMaxFrameRate = 30.0f;
    param.uiIntraPeriod = 60;
    param.bEnableBackgroundDetection = false;
    param.bEnableAdaptiveQuant = false;

    param.sSpatialLayers[0].uiProfileIdc = PRO_BASELINE;
    param.sSpatialLayers[0].uiLevelIdc   = LEVEL_3_1;
    param.sSpatialLayers[0].iVideoWidth  = width;
    param.sSpatialLayers[0].iVideoHeight = height;
    param.sSpatialLayers[0].fFrameRate   = 30.0f;
    param.sSpatialLayers[0].iSpatialBitrate = TARGET_BITRATE;

    encoder->InitializeExt(&param);
}

VideoEncoderInt16::~VideoEncoderInt16() {
    encoder->Uninitialize();
    WelsDestroySVCEncoder(encoder);
}

std::vector<uint8_t> VideoEncoderInt16::encodeFrame(const std::vector<int16_t> &frame) const {
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

int VideoEncoderInt16::getWidth() const {
    return width;
}

int VideoEncoderInt16::getHeight() const {
    return height;
}
