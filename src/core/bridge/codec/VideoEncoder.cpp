//
// Created by limo on 3/13/25.
//

#include "VideoEncoder.hpp"

#include <libyuv.h>
#include <stdexcept>
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

VideoEncoderAV1::VideoEncoderAV1(const int width, const int height) : VideoEncoder(width, height), encoder(nullptr) {
    EbSvtAv1EncConfiguration config{};
    EbErrorType err = svt_av1_enc_init_handle(&encoder, &config);
    if (err != EB_ErrorNone) {
        fprintf(stderr, "init_handle failed: %d\n", err);
        return;
    }

    config.source_width = width;
    config.source_height = height;
    config.forced_max_frame_width = width;
    config.forced_max_frame_height = height;
    config.target_bit_rate = 1000000;
    config.rate_control_mode = SVT_AV1_RC_MODE_CBR;
    config.frame_rate_numerator = 60;
    config.frame_rate_denominator = 1;
    config.intra_period_length = 60;
    config.encoder_bit_depth = 8;
    config.encoder_color_format = EB_YUV420;
    config.tune = 1;
    config.pred_structure = SVT_AV1_PRED_LOW_DELAY_B;
    config.qp = 35;

    err = svt_av1_enc_set_parameter(encoder, &config);
    if (err != EB_ErrorNone) {
        fprintf(stderr, "set_parameter failed: %d\n", err);
        return;
    }

    err = svt_av1_enc_init(encoder);
    if (err != EB_ErrorNone) {
        fprintf(stderr, "enc_init failed: %d\n", err);
        return;
    }
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

    EbSvtIOFormat io{};
    io.luma = y.data();
    io.cb = u.data();
    io.cr = v.data();

    io.y_stride = width;
    io.cb_stride = width / 2;
    io.cr_stride = width / 2;

    EbBufferHeaderType input_buffer{};
    input_buffer.p_buffer = reinterpret_cast<uint8_t*>(&io);
    input_buffer.n_alloc_len = sizeof(EbSvtIOFormat);
    input_buffer.n_filled_len = width * height * 3 / 2;
    input_buffer.flags = 0;
    input_buffer.pic_type = EB_AV1_INVALID_PICTURE;

    if (svt_av1_enc_send_picture(encoder, &input_buffer) != EB_ErrorNone)
        return {};

    std::vector<uint8_t> output;
    while (true) {
        EbBufferHeaderType *output_buffer = nullptr;
        if (const EbErrorType err = svt_av1_enc_get_packet(encoder, &output_buffer, 0);
                err == EB_ErrorNone && output_buffer) {
            output.assign(output_buffer->p_buffer, output_buffer->p_buffer + output_buffer->n_filled_len);
            svt_av1_enc_release_out_buffer(&output_buffer);
            break;
        }
    }
    return output;
}
