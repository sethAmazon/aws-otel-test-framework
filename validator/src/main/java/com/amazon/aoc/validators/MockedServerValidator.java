package com.amazon.aoc.validators;

import com.amazon.aoc.callers.ICaller;
import com.amazon.aoc.exception.BaseException;
import com.amazon.aoc.exception.ExceptionCode;
import com.amazon.aoc.fileconfigs.FileConfig;
import com.amazon.aoc.helpers.RetryHelper;
import com.amazon.aoc.models.Context;
import com.amazon.aoc.models.ValidationConfig;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.nio.charset.StandardCharsets;

@Log4j2
public class MockedServerValidator implements IValidator {
  String mockedServerValidatingUrl;
  ICaller caller;

  @Override
  public void init(
      Context context,
      ValidationConfig validationConfig,
      ICaller caller,
      FileConfig expectedDataTemplate)
      throws Exception {
    mockedServerValidatingUrl = context.getMockedServerValidatingUrl();
    this.caller = caller;
  }

  @Override
  public void validate() throws Exception {
    // hit the endpoint to generate data if need be
    if (caller != null) {
      caller.callSampleApp();
    }

    // hit the mocked server to validate to if it receives data
    callMockedServer();
  }

  private void callMockedServer() throws Exception {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(this.mockedServerValidatingUrl).build();

    RetryHelper.retry(
        () -> {
          Response response = client.newCall(request).execute();
          if (!response.isSuccessful()) {
            throw new BaseException(ExceptionCode.MOCKED_SERVER_NOT_AVAILABLE);
          }

          log.info("Response : {}", response);
          log.info("Response body : {}", response.body().bytes());
          log.info("Reponse body string : {}",
                  new String(response.body().bytes(), StandardCharsets.UTF_8));
          String responseBody = response.body().string();
          if (!responseBody.equalsIgnoreCase("success")) {
            throw new BaseException(ExceptionCode.MOCKED_SERVER_NOT_RECEIVE_DATA);
          }

          log.info("mocked server validation passed");
        });
  }
}
