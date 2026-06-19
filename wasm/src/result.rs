#![allow(dead_code)]

use prost::Message;

#[derive(Clone, PartialEq, Message)]
pub(crate) struct FormatTomlResult {
    #[prost(oneof = "format_toml_result::Result", tags = "1, 2")]
    pub(crate) result: Option<format_toml_result::Result>,
}

pub(crate) mod format_toml_result {
    use super::{FormatTomlFailure, FormatTomlSuccess};

    #[derive(Clone, PartialEq, prost::Oneof)]
    pub(crate) enum Result {
        #[prost(message, tag = "1")]
        Success(FormatTomlSuccess),
        #[prost(message, tag = "2")]
        Failure(FormatTomlFailure),
    }
}

#[derive(Clone, PartialEq, Message)]
pub(crate) struct FormatTomlSuccess {
    #[prost(string, tag = "1")]
    pub(crate) formatted: String,
}

#[derive(Clone, PartialEq, Message)]
pub(crate) struct FormatTomlFailure {
    #[prost(string, tag = "1")]
    pub(crate) message: String,
}

pub(crate) fn encode_success(formatted: impl Into<String>) -> Vec<u8> {
    FormatTomlResult {
        result: Some(format_toml_result::Result::Success(FormatTomlSuccess {
            formatted: formatted.into(),
        })),
    }
    .encode_to_vec()
}

pub(crate) fn encode_failure(message: impl Into<String>) -> Vec<u8> {
    FormatTomlResult {
        result: Some(format_toml_result::Result::Failure(FormatTomlFailure {
            message: message.into(),
        })),
    }
    .encode_to_vec()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn encodes_and_decodes_success_result() {
        let bytes = encode_success("key = \"value\"\n");
        let decoded = FormatTomlResult::decode(bytes.as_slice()).expect("success result decodes");

        match decoded.result {
            Some(format_toml_result::Result::Success(success)) => {
                assert_eq!(success.formatted, "key = \"value\"\n");
            }
            other => panic!("expected success result, got {other:?}"),
        }
    }

    #[test]
    fn encodes_and_decodes_failure_result() {
        let bytes = encode_failure("input was not valid UTF-8");
        let decoded = FormatTomlResult::decode(bytes.as_slice()).expect("failure result decodes");

        match decoded.result {
            Some(format_toml_result::Result::Failure(failure)) => {
                assert_eq!(failure.message, "input was not valid UTF-8");
            }
            other => panic!("expected failure result, got {other:?}"),
        }
    }
}
