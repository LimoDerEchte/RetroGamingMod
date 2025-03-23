//
// Created by limo on 3/13/25.
//

#pragma once
#include <cstdint>
#include <vector>

extern "C" {
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
}

#define FPS 30
#define BITRATE 250000

class VideoEncoderRGB565 {
    AVCodecContext* codec_ctx = nullptr;
    AVFrame* frame = nullptr;
    AVPacket* pkt = nullptr;
    SwsContext* sws_ctx = nullptr;
    const int width, height;
    int frame_count = 0;

public:
    VideoEncoderRGB565(int width, int height);
    ~VideoEncoderRGB565();

    std::vector<uint8_t> encode(uint16_t* data);
    void reset() const;
};
