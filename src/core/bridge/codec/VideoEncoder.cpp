//
// Created by limo on 3/13/25.
//

#include "VideoEncoder.hpp"

#include <iostream>

VideoEncoderInt16::VideoEncoderInt16(const int width, const int height) : width(width), height(height), encoder(nullptr) {
    WelsCreateSVCEncoder(&encoder);

    SEncParamExt param = {};
    encoder->GetDefaultParams(&param);

    param.iUsageType = SCREEN_CONTENT_REAL_TIME;
    param.iPicWidth = width;
    param.iPicHeight = height;
    param.iTargetBitrate = TARGET_BITRATE;
    param.iRCMode = RC_BITRATE_MODE;
    param.fMaxFrameRate = 60.0f;
    param.uiIntraPeriod = 60;
    param.bEnableBackgroundDetection = false;
    param.bEnableAdaptiveQuant = false;

    param.sSpatialLayers[0].uiProfileIdc = PRO_BASELINE;
    param.sSpatialLayers[0].uiLevelIdc   = LEVEL_3_1;
    param.sSpatialLayers[0].iVideoWidth  = width;
    param.sSpatialLayers[0].iVideoHeight = height;
    param.sSpatialLayers[0].fFrameRate   = 60.0f;
    param.sSpatialLayers[0].iSpatialBitrate = TARGET_BITRATE;

    encoder->InitializeExt(&param);
}

VideoEncoderInt16::~VideoEncoderInt16() {
    encoder->Uninitialize();
    WelsDestroySVCEncoder(encoder);
}

std::vector<uint8_t> VideoEncoderInt16::encodeFrame(const std::vector<int16_t> &frame) const {
    SSourcePicture pic = {};
    pic.iPicWidth = width;
    pic.iPicHeight = height;
    pic.iColorFormat = videoFormatI420;
    pic.iStride[0] = width;
    pic.iStride[1] = width / 2;
    pic.iStride[2] = width / 2;

    std::vector<uint8_t> y_plane(width * height);
    std::vector<uint8_t> u_plane(width * height / 4);
    std::vector<uint8_t> v_plane(width * height / 4);

    pic.pData[0] = y_plane.data();
    pic.pData[1] = u_plane.data();
    pic.pData[2] = v_plane.data();

    for (int j = 0; j < height; ++j) {
        for (int i = 0; i < width; ++i) {
            const int idx = j*width + i;
            const uint16_t pixel = frame[idx];

            uint8_t r = (pixel >> 11) & 0x1F; r = (r << 3) | (r >> 2);
            uint8_t g = (pixel >> 5) & 0x3F;  g = (g << 2) | (g >> 4);
            uint8_t b = pixel & 0x1F;         b = (b << 3) | (b >> 2);

            const auto y = static_cast<uint8_t>(0.299*r + 0.587*g + 0.114*b);
            y_plane[j*width + i] = y;

            if (j%2 ==0 && i%2 ==0) {
                int u_sum = 0, v_sum = 0;
                int count = 0;
                for (int jj=0;jj<2 && j+jj<height;++jj) {
                    for (int ii=0; ii<2 && i+ii<width; ++ii) {
                        int idx2 = (j+jj)*width + (i+ii);
                        uint16_t p2 = frame[idx2];
                        uint8_t r2 = (p2>>11)&0x1F; r2=(r2<<3)|(r2>>2);
                        uint8_t g2 = (p2>>5)&0x3F;  g2=(g2<<2)|(g2>>4);
                        uint8_t b2 = p2 &0x1F;      b2=(b2<<3)|(b2>>2);

                        u_sum += static_cast<int>(-0.169*r2 -0.331*g2 +0.5*b2 +128);
                        v_sum += static_cast<int>(0.5*r2 -0.419*g2 -0.081*b2 +128);
                        count++;
                    }
                }
                u_plane[(j/2)*(width/2) + (i/2)] = static_cast<uint8_t>(u_sum/count);
                v_plane[(j/2)*(width/2) + (i/2)] = static_cast<uint8_t>(v_sum/count);
            }
        }
    }

    // Encode frame
    SFrameBSInfo info = {};
    encoder->EncodeFrame(&pic, &info);
    std::cout << info.iFrameSizeInBytes << std::endl;

    // Collect NAL units
    std::vector<uint8_t> output;
    for (int i = 0; i < info.iLayerNum; ++i) {
        const SLayerBSInfo* layer = &info.sLayerInfo[i];
        uint8_t* p = layer->pBsBuf; // start of this layer

        for (int j = 0; j < layer->iNalCount; ++j) {
            output.insert(output.end(), p, p + layer->pNalLengthInByte[j]);
            p += layer->pNalLengthInByte[j]; // advance to next NAL
        }
    }
    std::cout << "Encoded " << output.size() << " bytes" << std::endl;
    return output;
}

int VideoEncoderInt16::getWidth() const {
    return width;
}

int VideoEncoderInt16::getHeight() const {
    return height;
}
