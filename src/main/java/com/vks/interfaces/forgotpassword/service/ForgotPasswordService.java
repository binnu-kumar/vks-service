package com.vks.interfaces.forgotpassword.service;

import com.vks.interfaces.forgotpassword.model.ForgotPasswordRequest;
import com.vks.interfaces.forgotpassword.model.ForgotPasswordResponse;
import com.vks.interfaces.forgotpassword.model.ResetPasswordWithTokenRequest;
import com.vks.interfaces.forgotpassword.model.VerifyOtpRequest;
import com.vks.interfaces.forgotpassword.model.VerifyOtpResponse;
import com.vks.interfaces.resetpassword.model.ResetPasswordResponse;

public interface ForgotPasswordService {

    ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request);

    VerifyOtpResponse verifyOtp(VerifyOtpRequest request);

    ResetPasswordResponse resetPasswordWithToken(ResetPasswordWithTokenRequest request);
}
