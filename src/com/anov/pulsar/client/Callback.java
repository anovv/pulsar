package com.anov.pulsar.client;

public interface Callback {
	
	public void onRequest();
	
	public void onMessage(String message);
	
	public void onServerUnavailable();
	
	public void onException(Exception e);
}	