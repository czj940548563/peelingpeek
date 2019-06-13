package com.czj.peelingpeek.peelingpeekgateway.response;

/**
 * @Author: clownc
 * @Date: 2019-06-12 16:16
 */
public class TokenForbiddenResponse extends BaseResponse {
    public TokenForbiddenResponse(String message) {
        super(RestCodeConstants.TOKEN_FORBIDDEN_CODE, message);
    }
}
