use rav1e::{Config, Context, EncoderConfig};
use rav1e::data::Rational;
use tracing::warn;
use yuv::{rgba_to_sharp_yuv420, BufferStoreMut, SharpYuvGammaTransfer, YuvPlanarImageMut, YuvRange, YuvStandardMatrix};

pub trait VideoEncoder {
    fn new(width: u32, height: u32) -> Self where Self: Sized;
    fn submit_frame(&mut self, data: Vec<u8>);
    fn retrieve_packet(&mut self) -> Option<Vec<u8>>;
}

pub struct VideoEncoderAV1 {
    width: usize,
    height: usize,
    encoder: Context<u8>
}

impl VideoEncoder for VideoEncoderAV1 {
    fn new(width: u32, height: u32) -> Self {
        let mut config = EncoderConfig::with_speed_preset(10);
        config.width = width as usize;
        config.height = height as usize;

        // Realtime settings
        config.low_latency = true;
        config.error_resilient = true;

        // FPS settings
        config.time_base = Rational::new(1, 60);
        config.min_key_frame_interval = 120;
        config.max_key_frame_interval = 120;

        // Rate Control
        config.quantizer = 100;
        config.bitrate = 800_000;

        config.tile_cols = 1;
        config.tile_rows = 1;

        Self {
            width: config.width,
            height: config.height,
            encoder: Config::new().with_encoder_config(config).new_context()
                .expect("Failed to initialize video encoder")
        }
    }

    fn submit_frame(&mut self, data: Vec<u8>) {
        let y = BufferStoreMut::Owned(vec![0u8; self.width * self.height]);
        let u = BufferStoreMut::Owned(vec![0u8; (self.width / 2) * (self.height / 2)]);
        let v = BufferStoreMut::Owned(vec![0u8; (self.width / 2) * (self.height / 2)]);

        let mut planar_image = YuvPlanarImageMut {
            width: self.width as u32, height: self.height as u32,
            y_plane: y, y_stride: self.width as u32,
            u_plane: u, u_stride: self.width as u32 / 2,
            v_plane: v, v_stride: self.width as u32 / 2,
        };

        if rgba_to_sharp_yuv420(
            &mut planar_image, data.as_slice(), 4,
            YuvRange::Limited, YuvStandardMatrix::Bt709, SharpYuvGammaTransfer::Rec709
        ).is_err() {
            warn!("Failed to sharp yuv420 frame");
            return;
        };

        let mut frame = self.encoder.new_frame();
        frame.planes[0].copy_from_raw_u8(planar_image.y_plane.borrow(), planar_image.y_stride as usize, 1);
        frame.planes[1].copy_from_raw_u8(planar_image.u_plane.borrow(), planar_image.u_stride as usize, 1);
        frame.planes[2].copy_from_raw_u8(planar_image.v_plane.borrow(), planar_image.v_stride as usize, 1);

        if self.encoder.send_frame(frame).is_err() {
            warn!("Failed to submit video frame to rav1e");
        }
    }

    fn retrieve_packet(&mut self) -> Option<Vec<u8>> {
        match self.encoder.receive_packet() {
            Ok(pak) => Some(pak.data),
            Err(_) => None
        }
    }
}
