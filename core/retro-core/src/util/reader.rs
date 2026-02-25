use std::ffi::c_void;
use std::ptr;
use rust_libretro_sys::retro_pixel_format;
use tracing::warn;

fn rgb565_to_rgb8888(pixel: u16) -> u32 {
    let r = ((pixel >> 11) & 0x1F) as u32;
    let g = ((pixel >> 5) & 0x3F) as u32;
    let b = (pixel & 0x1F) as u32;

    let r8 = (r * 0xFF / 0x1F) << 16;
    let g8 = (g * 0xFF / 0x3F) << 8;
    let b8 = b * 0xFF;

    0xFF000000 | r8 | g8 | b8
}

fn rgb1555_to_rgb8888(pixel: u16) -> u32 {
    let r = ((pixel >> 10) & 0x1F) as u32;
    let g = ((pixel >> 5) & 0x1F) as u32;
    let b = (pixel & 0x1F) as u32;

    let r8 = (r * 0xFF / 0x1F) << 16;
    let g8 = (g * 0xFF / 0x1F) << 8;
    let b8 = b * 0xFF;

    0xFF000000 | r8 | g8 | b8
}

pub fn convert_data(format: retro_pixel_format, data: *const c_void, width: u32, height: u32, pitch: u32, dst: &mut [u32]) {
    let data = data as *const u8;
    match format {
        retro_pixel_format::RETRO_PIXEL_FORMAT_XRGB8888 => {
            if pitch == width * 4 {
                let src_slice = unsafe {
                    std::slice::from_raw_parts(data as *const u32, (width * height) as usize)
                };
                dst.copy_from_slice(src_slice);
            } else {
                for y in 0..height {
                    let row = unsafe { data.add((y * pitch) as usize) };
                    for x in 0..width {
                        dst[(y * width + x) as usize] = unsafe {
                            ptr::read_unaligned(row.add((x * 4) as usize) as *const u32)
                        };
                    }
                }
            }
        }
        retro_pixel_format::RETRO_PIXEL_FORMAT_RGB565 | retro_pixel_format::RETRO_PIXEL_FORMAT_0RGB1555 => {
            let mut converter: fn(pixel: u16) -> u32 = rgb565_to_rgb8888;
            if format == retro_pixel_format::RETRO_PIXEL_FORMAT_0RGB1555 {
                converter = rgb1555_to_rgb8888;
            }

            for y in 0..height {
                let row = unsafe { data.add((y * pitch) as usize) };
                for x in 0..width {
                    let val = unsafe {
                        ptr::read_unaligned(row.add((x * 2) as usize) as *const u16)
                    };
                    dst[(y * width + x) as usize] = converter(val)
                }
            }
        },
        _ => {
            warn!("Unknown pixel format: {:?}", format);
        }
    }
}
