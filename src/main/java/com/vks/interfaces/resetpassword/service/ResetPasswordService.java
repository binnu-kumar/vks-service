package com.vks.interfaces.resetpassword.service;

import com.vks.interfaces.resetpassword.model.ResetPasswordRequest;
import com.vks.interfaces.resetpassword.model.ResetPasswordResponse;

public interface ResetPasswordService {

    ResetPasswordResponse resetPassword(ResetPasswordRequest request);
}
