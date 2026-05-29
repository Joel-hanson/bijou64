use jni::objects::{JClass, JByteArray};
use jni::sys::{jbyteArray, jlong};
use jni::JNIEnv;

#[no_mangle]
pub extern "system" fn Java_org_bijou64_Bijou64_encodeNative(
    mut env: JNIEnv,
    _class: JClass,
    value: jlong,
) -> jbyteArray {
    let mut buffer = Vec::new();
    bijou64::encode(value as u64, &mut buffer);

    let array = match env.new_byte_array(buffer.len() as i32) {
        Ok(array) => array,
        Err(error) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("JNI encode failed: {}", error),
            );
            return std::ptr::null_mut();
        }
    };

    let bytes: Vec<i8> = buffer.iter().map(|&value| value as i8).collect();
    if let Err(error) = env.set_byte_array_region(&array, 0, &bytes) {
        let _ = env.throw_new(
            "java/lang/RuntimeException",
            format!("JNI encode failed: {}", error),
        );
        return std::ptr::null_mut();
    }

    **array
}

#[no_mangle]
pub extern "system" fn Java_org_bijou64_Bijou64_decodeNative(
    mut env: JNIEnv,
    _class: JClass,
    bytes: JByteArray,
) -> jlong {
    let length = match env.get_array_length(&bytes) {
        Ok(len) => len,
        Err(error) => {
            let _ = env.throw_new(
                "java/lang/RuntimeException",
                format!("JNI decode failed: {}", error),
            );
            return 0;
        }
    };

    let mut buffer = vec![0i8; length as usize];
    if let Err(error) = env.get_byte_array_region(bytes, 0, &mut buffer) {
        let _ = env.throw_new(
            "java/lang/RuntimeException",
            format!("JNI decode failed: {}", error),
        );
        return 0;
    }

    let bytes: Vec<u8> = buffer.iter().map(|&value| value as u8).collect();
    match bijou64::decode(&bytes) {
        Ok((value, _)) => value as jlong,
        Err(error) => {
            let _ = env.throw_new(
                "java/lang/IllegalArgumentException",
                format!("bijou64 decode error: {}", error),
            );
            0
        }
    }
}
