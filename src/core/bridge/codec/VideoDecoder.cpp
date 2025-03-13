//
// Created by limo on 3/13/25.
//

#include "VideoDecoder.hpp"

#include <iostream>

VideoDecoderARGB::VideoDecoderARGB(const int width, const int height) : width(width), height(height) {
    const AVCodec *codec = avcodec_find_decoder(AV_CODEC_ID_H264);
    if (!codec) {
        std::cerr << "H.264 decoder not found\n";
        exit(1);
    }

    codec_ctx = avcodec_alloc_context3(codec);
    codec_ctx->width = width;
    codec_ctx->height = height;
    codec_ctx->pix_fmt = AV_PIX_FMT_YUV420P;

    if (avcodec_open2(codec_ctx, codec, nullptr) < 0) {
        std::cerr << "Could not open H.264 decoder\n";
        exit(1);
    }

    frame = av_frame_alloc();
    pkt = av_packet_alloc();
}

VideoDecoderARGB::~VideoDecoderARGB() {
    avcodec_free_context(&codec_ctx);
    av_frame_free(&frame);
    av_packet_free(&pkt);
}

bool VideoDecoderARGB::decode(const std::vector<uint8_t>& encoded_data, uint32_t* output_buffer) const {
    if (pkt == nullptr || frame == nullptr) {
        std::cerr << "Called decode before initialization\n";
        return false;
    }
    av_packet_unref(pkt);
    pkt->data = const_cast<uint8_t*>(encoded_data.data());
    pkt->size = static_cast<int>(encoded_data.size());

    int ret = avcodec_send_packet(codec_ctx, pkt);
    if (ret < 0) {
        std::cerr << "Error sending packet for decoding\n";
        return false;
    }

    ret = avcodec_receive_frame(codec_ctx, frame);
    if (ret < 0) {
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
            return false;
        }
        std::cerr << "Error receiving frame from decoder\n";
        return false;
    }

    SwsContext* sws_ctx = sws_getContext(
        width, height, AV_PIX_FMT_YUV420P,
        width, height, AV_PIX_FMT_BGRA,
        SWS_BILINEAR, nullptr, nullptr, nullptr
    );
    if (!sws_ctx) {
        std::cerr << "Could not initialize scaling context\n";
        return false;
    }

    uint8_t* dst_slices[1] = { reinterpret_cast<uint8_t*>(output_buffer) };
    const int dst_stride[1] = { width * 4 };
    sws_scale(sws_ctx, frame->data, frame->linesize, 0, height, dst_slices, dst_stride);
    sws_freeContext(sws_ctx);
    return true;
}
