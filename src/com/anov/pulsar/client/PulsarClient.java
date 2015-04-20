package com.anov.pulsar.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PulsarClient {
	
	private static final long DEFAULT_PERIOD = 60*60;//default period in s
	private static final int SOCKET_TIMEOUT = 30*1000;//default socket timeout in ms
	
	private ScheduledExecutorService scheduledEcexutor;
	private Future<?> future;
	private Callback callback;
	private String ip;
	private int port;
	private long period = DEFAULT_PERIOD;
	
	public PulsarClient(String ip, int port, long period, Callback callback){
		this.ip = ip;
		this.port = port;
		this.period = period;
		this.callback = callback;
	}
	
	public void setPeriodAndRestart(long period){
		stop();
		this.period = period;
		start();
	}
	
	public void start(){
		stop();
		scheduledEcexutor = Executors.newScheduledThreadPool(1);
		future = scheduledEcexutor.scheduleAtFixedRate(new Runnable(){

			@Override
			public void run() {
				sendRequest();
			}
			
		}, 0, period, TimeUnit.SECONDS);
	}
	
	public void stop(){
		if(future != null){
			future.cancel(true);
		}
		future = null;
		if(scheduledEcexutor != null){
			scheduledEcexutor.shutdown();
		}
		scheduledEcexutor = null;
	}
	
	private void sendRequest(){
		Socket client = null;  
		BufferedReader reader = null;
		try{
			client = new Socket();
			client.connect(new InetSocketAddress(ip, port), SOCKET_TIMEOUT);
			reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
		}catch(UnknownHostException e){
			callback.onServerUnavailable();
		}catch(IOException e){
			callback.onException(e);
		}
		
		if(client != null && reader != null){
			try {
				String message = "";
				String aux = "";
				while((aux = reader.readLine()) != null){
					message += aux + "\n";
				}
				client.close();
				reader.close();
				if(message.length() > 0){
					message = message.substring(0, message.length() - 1);
				}
				callback.onMessage(message);
			} catch (IOException e) {
				callback.onException(e);
			}
		}
		callback.onRequest();
	}
}


