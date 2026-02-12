//
// Created by limo on 3/13/25.
//

#include "VideoEncoder.hpp"

#include <libyuv.h>
#include <stdexcept>
#include <webp/encode.h>

#include <svt-av1/EbSvtAv1Enc.h>

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

VideoEncoderAV1::VideoEncoderAV1(const int width, const int height) : VideoEncoder(width, height), encoder(nullptr) {
    EbSvtAv1EncConfiguration config{};
    config.source_width = width;
    config.source_height = height;
    config.target_bit_rate = 1000000;
    config.rate_control_mode = SVT_AV1_RC_MODE_CQP_OR_CRF;
    config.frame_rate_numerator = 60;
    config.frame_rate_denominator = 1;
    config.intra_period_length = 60;
    config.encoder_bit_depth = 8;
    config.encoder_color_format = EB_YUV420;
    config.tune = 0;
    config.pred_structure = SVT_AV1_PRED_LOW_DELAY_B;
    config.qp = 35;

    svt_av1_enc_init_handle(&encoder, &config);
    svt_av1_enc_set_parameter(encoder, &config);
    svt_av1_enc_init(encoder);
}

VideoEncoderAV1::~VideoEncoderAV1() {
    svt_av1_enc_deinit(encoder);
    svt_av1_enc_deinit_handle(encoder);
}

std::vector<uint8_t> VideoEncoderAV1::encodeFrameRGB565(const std::vector<int16_t> &frame) const {
    std::vector<uint8_t> y(width*height), u(width*height/4), v(width*height/4);
    libyuv::RGB565ToI420(reinterpret_cast<const uint8_t*>(frame.data()), width*2,
                       y.data(), width,
                       u.data(), width/2,
                       v.data(), width/2,
                       width, height);

    EbBufferHeaderType input_buffer{};
    input_buffer.p_buffer = reinterpret_cast<uint8_t*>(&input_buffer);
    input_buffer.size = sizeof(input_buffer);
    input_buffer.n_filled_len = 0;
    input_buffer.p_app_private = nullptr;
    input_buffer.pic_type = EB_AV1_INVALID_PICTURE;

    auto *input_pic = reinterpret_cast<EbSvtIOFormat*>(input_buffer.p_buffer);
    input_pic->y_stride = width;
    input_pic->cb_stride = width/2;
    input_pic->cr_stride = width/2;
    input_pic->luma = y.data();
    input_pic->cb = u.data();
    input_pic->cr = v.data();

    svt_av1_enc_send_picture(encoder, &input_buffer);

    EbBufferHeaderType *output_buffer = nullptr;
    svt_av1_enc_get_packet(encoder, &output_buffer, 0);

    std::vector<uint8_t> output;
    if (output_buffer) {
        output.assign(output_buffer->p_buffer, output_buffer->p_buffer + output_buffer->n_filled_len);
        svt_av1_enc_release_out_buffer(&output_buffer);
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
