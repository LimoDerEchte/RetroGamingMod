
pub struct NativeImage {
    data: Vec<i32>,

    changed: bool,
    width: i32,
    height: i32,
    codec: i32, // TODO: Parse to a codec enum
}

impl NativeImage {
    pub fn new(width: i32, height: i32, codec: i32) -> Self {
        NativeImage {
            data: vec![0; (width * height) as usize],
            changed: false,
            width,
            height,
            codec,
        }
    }

    pub fn changed(&self) -> bool {
        self.changed
    }

    pub fn native_data_pointer(&self) -> i32 {
        self.data.as_ptr() as i32
    }
}
