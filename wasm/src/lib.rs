mod result;

use std::{mem, slice, str};

use taplo::formatter::{format, Options};

use crate::result::{encode_failure, encode_success};

#[cfg(all(target_family = "wasm", target_os = "unknown"))]
fn deterministic_getrandom(dest: &mut [u8]) -> Result<(), getrandom::Error> {
    for (index, byte) in dest.iter_mut().enumerate() {
        *byte = (index as u8).wrapping_mul(37).wrapping_add(113);
    }
    Ok(())
}

#[cfg(all(target_family = "wasm", target_os = "unknown"))]
getrandom::register_custom_getrandom!(deterministic_getrandom);

/// Allocates `size` bytes in WASM linear memory and transfers ownership to the host.
///
/// The host must later pass the returned pointer and the same size to `dealloc`.
#[no_mangle]
pub extern "C" fn alloc(size: usize) -> *mut u8 {
    let mut buffer = vec![0_u8; size].into_boxed_slice();
    let ptr = buffer.as_mut_ptr();
    mem::forget(buffer);
    ptr
}

/// Releases a buffer previously returned by `alloc` or `format_toml`.
///
/// # Safety contract
///
/// `ptr` must have been returned by this module with exactly `size` bytes. Passing any other
/// pointer/size pair is host misuse and has undefined behavior.
#[no_mangle]
pub extern "C" fn dealloc(ptr: *mut u8, size: usize) {
    if ptr.is_null() {
        return;
    }

    unsafe {
        drop(Box::from_raw(slice::from_raw_parts_mut(ptr, size)));
    }
}

/// Formats TOML bytes and returns `(result_ptr << 32) | result_len`.
///
/// The returned buffer contains a serialized `FormatTomlResult` protobuf message. Ownership of
/// that buffer is transferred to the host, which must later call `dealloc(result_ptr, result_len)`.
///
/// # Safety contract
///
/// `ptr` and `len` must identify a readable byte range in WASM linear memory.
#[no_mangle]
pub extern "C" fn format_toml(ptr: *mut u8, len: usize) -> u64 {
    let input = unsafe { slice::from_raw_parts(ptr.cast_const(), len) };
    leak_result(format_toml_bytes(input))
}

fn format_toml_bytes(input: &[u8]) -> Vec<u8> {
    match str::from_utf8(input) {
        Ok(source) => encode_success(format(source, Options::default())),
        Err(error) => encode_failure(format!("input was not valid UTF-8: {error}")),
    }
}

fn leak_result(bytes: Vec<u8>) -> u64 {
    let len = bytes.len();
    let mut buffer = bytes.into_boxed_slice();
    let ptr = buffer.as_mut_ptr();
    mem::forget(buffer);

    ((ptr as u64) << 32) | (len as u64)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::result::{format_toml_result, FormatTomlResult};
    use prost::Message;

    #[test]
    fn formats_valid_utf8_as_success() {
        let input = b"key=\"value\"";
        let decoded = decode_result(format_toml_bytes(input));

        match decoded.result {
            Some(format_toml_result::Result::Success(success)) => {
                assert_eq!(success.formatted, "key = \"value\"\n");
            }
            other => panic!("expected success result, got {other:?}"),
        }
    }

    #[test]
    fn invalid_utf8_returns_failure() {
        let input = [0xff_u8];
        let decoded = decode_result(format_toml_bytes(&input));

        match decoded.result {
            Some(format_toml_result::Result::Failure(failure)) => {
                assert!(failure.message.contains("not valid UTF-8"));
            }
            other => panic!("expected failure result, got {other:?}"),
        }
    }

    fn decode_result(bytes: Vec<u8>) -> FormatTomlResult {
        FormatTomlResult::decode(bytes.as_slice()).expect("result protobuf decodes")
    }
}
