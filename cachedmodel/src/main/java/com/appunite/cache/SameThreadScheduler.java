package com.appunite.cache;

public class SameThreadScheduler implements Scheduler {
    @Override
    public void schedule(Runnable runnable) {
        runnable.run();
    }
}
