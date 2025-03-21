//
// Created by limo on 3/13/25.
//

#include "VideoEncoder.hpp"

#include <iostream>

VideoEncoderRGB565::VideoEncoderRGB565(const int width, const int height) : width(width), height(height) {
    const AVCodec *codec = avcodec_find_encoder(AV_CODEC_ID_H264);
    if (!codec) {
        std::cerr << "[VideoEncoder] H.264 codec not found\n";
        exit(1);
    }
    codec_ctx = avcodec_alloc_context3(codec);
    codec_ctx->bit_rate = BITRATE;
    codec_ctx->width = width;
    codec_ctx->height = height;
    codec_ctx->time_base = {1, FPS};
    codec_ctx->framerate = {FPS, 1};
    codec_ctx->gop_size = 30;
    codec_ctx->pix_fmt = AV_PIX_FMT_YUV420P;
    codec_ctx->max_b_frames = 0;

    av_opt_set(codec_ctx->priv_data, "preset", "ultrafast", 0);
    av_opt_set(codec_ctx->priv_data, "tune", "zerolatency", 0);

    if (avcodec_open2(codec_ctx, codec, nullptr) < 0) {
        std::cerr << "[VideoEncoder] Could not open H.264 codec\n";
        exit(1);
    }
    frame = av_frame_alloc();
    frame->format = AV_PIX_FMT_YUV420P;
    frame->width = width;
    frame->height = height;
    av_frame_get_buffer(frame, 32);
    pkt = av_packet_alloc();
}

VideoEncoderRGB565::~VideoEncoderRGB565() {
    avcodec_free_context(&codec_ctx);
    av_frame_free(&frame);
    av_packet_free(&pkt);
}

std::vector<uint8_t> VideoEncoderRGB565::encode(uint16_t *data) const {
    std::cerr << "DBG ENC " << width << "x" << height << std::endl;
    std::cerr << "DBG 1" << std::endl;
    std::vector<uint8_t> encoded_data;
    if (pkt == nullptr || frame == nullptr) {
        std::cerr << "[VideoEncoder] Called encode before initialization";
        return encoded_data;
    }
    std::cerr << "DBG 2" << std::endl;
    SwsContext* sws_ctx = nullptr;
    sws_ctx = sws_getContext(width, height, AV_PIX_FMT_RGB565, width, height,
        AV_PIX_FMT_YUV420P, SWS_BILINEAR, nullptr, nullptr, nullptr);
    std::cerr << "DBG 2.5" << std::endl;
    if (sws_ctx == nullptr) {
        std::cerr << "[VideoEncoder] Could not initialize sws context" << std::endl;
        return encoded_data;
    }
    std::cerr << "DBG 3" << std::endl;
    uint8_t* src_slices[1] = { reinterpret_cast<uint8_t*>(data) };
    const int src_stride[1] = { 2 * width };
    sws_scale(sws_ctx, src_slices, src_stride, 0, height, frame->data, frame->linesize);
    std::cerr << "DBG 4" << std::endl;
    if (avcodec_send_frame(codec_ctx, frame) == 0) {
        while (avcodec_receive_packet(codec_ctx, pkt) == 0) {
            encoded_data.assign(pkt->data, pkt->data + pkt->size);
            av_packet_unref(pkt);
        }
    }
    std::cerr << "DBG 5" << std::endl;
    sws_freeContext(sws_ctx);
    std::cerr << "DBG ENC FIN" << std::endl;
    return encoded_data;
}
