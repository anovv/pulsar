package com.anov.pulsar.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicLong;

public class PulsarServer {
	
	private volatile static boolean isRunning = false;
	
	private AtomicLong servedCounter;
	
	private static int port;
	private Selector selector;
	private ServerSocketChannel serverChannel;
	private ByteBuffer buffer = ByteBuffer.allocate(1024);
	
	private static Thread mainThread;
	private static Thread inputThread;
		
	public static void main(String[] args){
		if(args.length != 2){
			System.err.println("Wrong number of args");
			System.exit(1);
		}
		try{
			port = Integer.parseInt(args[0]);
		}catch(Exception e){
			System.err.println("Wrong port number formatting");
			System.exit(1);
		}
		String path = args[1];
		String info = "";
		File file = new File(path);
		try{
			Scanner s = new Scanner(file);
			while(s.hasNext()){
				String next = s.nextLine();
				info += next + "\n";
			}
			s.close();
			if(info.length() > 0){
				info = info.substring(0, info.length() - 1);
			}
		}catch (Exception e) {
			System.err.print("Can not open the file");
			System.exit(1);
		} 
		System.out.println(info);
		final PulsarServer pulsarServer = new PulsarServer(port, info);
		isRunning = true;
		inputThread = new Thread(new Runnable(){

			@Override
			public void run() {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				while(isRunning){
					try{
						String command = br.readLine();
						switch(command){
							default:
								System.out.println("Invalid command");
								break;
							case "exit" :
								pulsarServer.exit();
								break;				
						}
					}catch(Exception e){
						handleException(e);
					}					
				}
			}
			
		});
		inputThread.start();
		pulsarServer.start();
	}
		
	public PulsarServer(int port, String info){
		buffer.clear();
		buffer.put(info.getBytes());
		buffer.flip();
		try{
			selector = Selector.open();
			serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			serverChannel.socket().bind(new InetSocketAddress(port));
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
		}catch(Exception e){
			handleException(e);
		}
	}
		
	public void start(){
		servedCounter = new AtomicLong(0);
		
		System.out.println("Server is running at port " + port + "...");
		mainThread = new Thread(new Runnable(){

			@Override
			public void run() {
				while(isRunning){
					try{
						int selected = selector.select();
						if(selected == 0){
							continue;
						}
						
						Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
						
						while(isRunning && iterator.hasNext()){
							SelectionKey key = iterator.next();
							iterator.remove();
							SocketChannel clientChannel;
							switch(key.readyOps()){
								case SelectionKey.OP_ACCEPT:
									clientChannel = ((ServerSocketChannel) key.channel()).accept();
									clientChannel.configureBlocking(false);
									clientChannel.register(selector, SelectionKey.OP_WRITE);
									break;
								case SelectionKey.OP_WRITE:
									clientChannel = (SocketChannel) key.channel();
									while(buffer.hasRemaining()){
										clientChannel.write(buffer);
									}
									buffer.clear();
									clientChannel.close();
									long served = servedCounter.incrementAndGet();
									System.out.println(served + " clients served so far");
									break;
							}
						}
					}catch(Exception e){
						handleException(e);
					}
				}
			}
		});
		mainThread.start();
	}
	
	public void exit(){
		isRunning = false;
		mainThread = null;
		inputThread = null;
		System.exit(0);
	}
	
	private static void handleException(Exception e){
		System.err.print(e.toString());
		isRunning = false;
		mainThread = null;
		inputThread = null;
		System.exit(1);
	}
}
