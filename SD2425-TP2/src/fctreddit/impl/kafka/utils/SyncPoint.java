package fctreddit.impl.kafka.utils;

import fctreddit.api.java.Result;

import java.util.concurrent.ConcurrentHashMap;

public class SyncPoint {
	
	private final ConcurrentHashMap<Long, Result> result;
	private long version;
	
	private SyncPoint() {
		this.result = new ConcurrentHashMap<>();
		this.version = -1;
	}
	
	private static SyncPoint instance = null;
	
	public static SyncPoint getSyncPoint() {
		if(SyncPoint.instance == null)
			SyncPoint.instance = new SyncPoint();
		
		return SyncPoint.instance;
	}
	
	public synchronized Result waitForResult(long n ) {
		while( version < n ) {
			try {
				wait();
			} catch (InterruptedException e) {
				// nothing to be done here
			}
		}
		
		return result.remove(n);
	}

	public synchronized void waitForClientVersion() {
		Long clientVersion = VersionFilter.version.get();

		if (clientVersion != null){
			while (version < clientVersion) {
				try {
					wait();
				} catch (InterruptedException e) {
					// nothing to be done here
				}
			}
		}
	}

	
	public synchronized void setResult( long n, Result res ) {
		result.put(n, res);
		version = n;
		notifyAll();
	}

	public long getVersion() {
		return version;
	}
}