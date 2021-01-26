package com.braintreepayments.api;

import android.os.Handler;

import com.braintreepayments.api.ThreadScheduler;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class ThreadSchedulerTest {

    Handler mainThreadHandler;
    ExecutorService backgroundThreadPool;

    @Before
    public void beforeEach() {
        mainThreadHandler = mock(Handler.class);
        backgroundThreadPool = mock(ExecutorService.class);
    }

    @Test
    public void runOnBackground_submitsRunnableToThreadPool() {
        ThreadScheduler sut = new ThreadScheduler(mainThreadHandler, backgroundThreadPool);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {}
        };

        sut.runOnBackground(runnable);

        verify(backgroundThreadPool).submit(runnable);
    }


    @Test
    public void runOnMain_postsRunnableToHandler() {
        ThreadScheduler sut = new ThreadScheduler(mainThreadHandler, backgroundThreadPool);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {}
        };

        sut.runOnMain(runnable);

        verify(mainThreadHandler).post(runnable);
    }
}