/*
 * Copyright 2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.faulttolerance.async.circuitbreaker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class AsynchronousCircuitBreakerTest {
    @Test
    public void testAsyncCircuitBreaker(AsyncHelloService helloService) {
        AsyncHelloService.COUNTER.set(0);

        for (int i = 0; i < AsyncHelloService.THRESHOLD; i++) {
            assertThatThrownBy(() -> {
                helloService.hello(AsyncHelloService.Result.FAILURE).get();
            }).isExactlyInstanceOf(ExecutionException.class).hasCauseExactlyInstanceOf(IOException.class);
        }

        // Circuit should be open now
        assertThatThrownBy(() -> {
            helloService.hello(AsyncHelloService.Result.SUCCESS).get();
        }).isExactlyInstanceOf(ExecutionException.class).hasCauseExactlyInstanceOf(CircuitBreakerOpenException.class);

        assertThat(AsyncHelloService.COUNTER.get()).isEqualTo(AsyncHelloService.THRESHOLD);

        await().atMost(AsyncHelloService.DELAY * 2, TimeUnit.MILLISECONDS).untilAsserted(() -> {
            try {
                assertThat(helloService.hello(AsyncHelloService.Result.SUCCESS).get()).isEqualTo(AsyncHelloService.OK);
            } catch (ExecutionException e) {
                if (!(e.getCause() instanceof CircuitBreakerOpenException)) {
                    // CircuitBreakerOpenException is expected
                    throw e;
                }
            }
        });
    }

}
