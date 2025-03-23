//
// Created by limo on 3/13/25.
//

#include "VideoEncoder.hpp"

#include <iostream>
extern "C" {
#include <libavutil/opt.h>
#include <libavutil/imgutils.h>
}

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
    av_opt_set(codec_ctx->priv_data, "profile", "baseline", 0);

    if (avcodec_open2(codec_ctx, codec, nullptr) < 0) {
        std::cerr << "[VideoEncoder] Could not open H.264 codec\n";
        exit(1);
    }
    frame = av_frame_alloc();
    if (!frame) {
        std::cerr << "[VideoEncoder] Failed to allocate frame\n";
        exit(1);
    }
    frame->format = AV_PIX_FMT_YUV420P;
    frame->width = width;
    frame->height = height;
    int ret = av_frame_get_buffer(frame, 0);
    if (ret < 0) {
        char errbuf[AV_ERROR_MAX_STRING_SIZE];
        av_strerror(ret, errbuf, AV_ERROR_MAX_STRING_SIZE);
        std::cerr << "[VideoEncoder] Could not allocate frame buffers: " << errbuf << "\n";
        exit(1);
    }
    pkt = av_packet_alloc();
    if (!pkt) {
        std::cerr << "[VideoEncoder] Failed to allocate packet\n";
        exit(1);
    }
    sws_ctx = sws_getContext(width, height, AV_PIX_FMT_RGB565, width, height, AV_PIX_FMT_YUV420P, SWS_BILINEAR, nullptr, nullptr, nullptr);
    if (sws_ctx == nullptr) {
        std::cerr << "[VideoEncoder] Could not initialize sws context" << std::endl;
        exit(1);
    }
}

VideoEncoderRGB565::~VideoEncoderRGB565() {
    avcodec_free_context(&codec_ctx);
    av_frame_free(&frame);
    av_packet_free(&pkt);
    if (sws_ctx != nullptr) {
        sws_freeContext(sws_ctx);
        sws_ctx = nullptr;
    }
}

std::vector<uint8_t> VideoEncoderRGB565::encode(uint16_t *data) {
    std::cerr << "DBG ENC " << width << "x" << height << std::endl;
    std::vector<uint8_t> encoded_data;
    if (pkt == nullptr || frame == nullptr || sws_ctx == nullptr) {
        std::cerr << "[VideoEncoder] Called encode before initialization";
        return encoded_data;
    }
    int ret = av_frame_make_writable(frame);
    if (ret < 0) {
        char errbuf[AV_ERROR_MAX_STRING_SIZE];
        av_strerror(ret, errbuf, AV_ERROR_MAX_STRING_SIZE);
        std::cerr << "[VideoEncoder] Could not make frame writable: " << errbuf << "\n";
        reset();
        return encoded_data;
    }
    frame->pts = frame_count++;
    uint8_t* src_slices[1] = { reinterpret_cast<uint8_t*>(data) };
    const int src_stride[1] = { 2 * width };
    ret = sws_scale(sws_ctx, src_slices, src_stride, 0, height, frame->data, frame->linesize);
    if (ret < 0) {
        std::cerr << "[VideoEncoder] Error converting frame format\n";
        reset();
        return encoded_data;
    }
    std::cerr << "DBG 4" << std::endl;
    ret = avcodec_send_frame(codec_ctx, frame);
    if (ret < 0) {
        char errbuf[AV_ERROR_MAX_STRING_SIZE];
        av_strerror(ret, errbuf, AV_ERROR_MAX_STRING_SIZE);
        std::cerr << "[VideoEncoder] Error sending frame to encoder: " << errbuf << "\n";
        reset();
        return encoded_data;
    }
    std::cerr << "DBG 5" << std::endl;
    ret = avcodec_receive_packet(codec_ctx, pkt);
    if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
    	return encoded_data;
    } else if (ret < 0) {
        char errbuf[AV_ERROR_MAX_STRING_SIZE];
        av_strerror(ret, errbuf, AV_ERROR_MAX_STRING_SIZE);
        std::cerr << "[VideoEncoder] Error receiving packet: " << errbuf << "\n";
        reset();
    	return encoded_data;
    }
    encoded_data.assign(pkt->data, pkt->data + pkt->size);
    av_packet_unref(pkt);
    std::cerr << "DBG ENC FIN" << std::endl;
    return encoded_data;
}

void VideoEncoderRGB565::reset() {
    avcodec_flush_buffers(codec_ctx);
}
