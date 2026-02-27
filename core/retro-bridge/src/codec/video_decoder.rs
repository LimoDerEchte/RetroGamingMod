use dav1d::PlanarImageComponent;
use tracing::warn;
use yuv::{yuv420_to_bgr, YuvPlanarImage, YuvRange, YuvStandardMatrix};

pub trait VideoDecoder: Send {
    fn new(width: u32, height: u32) -> Self where Self: Sized;
    fn submit_packet(&mut self, data: Vec<u8>);
    fn retrieve_frame(&mut self) -> Option<Vec<u8>>;
}

pub struct VideoDecoderAV1 {
    width: u32,
    height: u32,
    decoder: dav1d::Decoder,
}

impl VideoDecoder for VideoDecoderAV1 {
    fn new(width: u32, height: u32) -> Self {
        let mut settings = dav1d::Settings::new();
        settings.set_n_threads(4);
        settings.set_max_frame_delay(1);

        Self {
            width,
            height,
            decoder: dav1d::Decoder::with_settings(&settings)
                .expect("Failed to initialize video decoder")
        }
    }

    fn submit_packet(&mut self, data: Vec<u8>) {
        if self.decoder.send_data(data, None, None, None).is_err() {
            warn!("Failed to submit video packet to dav1d");
        }
    }

    fn retrieve_frame(&mut self) -> Option<Vec<u8>> {
        let mut data = match self.decoder.get_picture() {
            Ok(picture) => picture,
            Err(_) => return None,
        };

        while let Ok(picture) = self.decoder.get_picture() {
            data = picture;
        }

        let planar_image = YuvPlanarImage {
            y_plane: &data.plane(PlanarImageComponent::Y), y_stride: data.stride(PlanarImageComponent::Y),
            u_plane: &data.plane(PlanarImageComponent::U), u_stride: data.stride(PlanarImageComponent::U),
            v_plane: &data.plane(PlanarImageComponent::V), v_stride: data.stride(PlanarImageComponent::V),
            width: self.width, height: self.height,
        };

        let mut bgr = vec![0u8; (self.width * self.height * 3) as usize];

        if yuv420_to_bgr(
            &planar_image, bgr.as_mut_slice(), 3,
            YuvRange::Limited, YuvStandardMatrix::Bt709
        ).is_err() { return None; }

        Some(bgr)
    }
}
