//
// Created by limo on 3/13/25.
//

#pragma once
#include <cstdint>
#include <vector>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
}

class VideoDecoderARGB {
    AVCodecContext* codec_ctx = nullptr;
    AVFrame* frame = nullptr;
    AVPacket* pkt = nullptr;
    const int width, height;

public:
    VideoDecoderARGB(int width, int height);
    ~VideoDecoderARGB();

    bool decode(const std::vector<uint8_t>& encoded_data, uint32_t* output_buffer) const;
};