package com.pinggusoft.async;

public interface AsyncExecutorAware<T> {

	public void setAsyncExecutor(AsyncExecutor<T> asyncExecutor);

}