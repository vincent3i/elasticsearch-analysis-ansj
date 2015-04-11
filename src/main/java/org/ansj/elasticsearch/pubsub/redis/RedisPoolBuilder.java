package org.ansj.elasticsearch.pubsub.redis;

import org.ansj.elasticsearch.crypt.SimpleCrypt;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Protocol;

public class RedisPoolBuilder {

	private static ESLogger logger = Loggers.getLogger("ansj-redis-pool");

	/**
	 * 控制一个pool可分配多少个jedis实例
	 * 如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)
	 */
	private int maxActive = 20;
	/**
	 * 控制一个pool最多有多少个状态为idle(空闲的)的jedis实例
	 */
	private int maxIdle = 10;
	/**
	 * 最大等待的毫秒数
	 */
	private int maxWait = 10 * 1000;
	/**
	 * 在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的
	 */
	private boolean testOnBorrow = true;
	//If redis must auth
	private String auth;
	private boolean isEncrypted;

	private String ipAddress = "127.0.0.1:6379";
	private int port = Protocol.DEFAULT_PORT;

	public int getMaxActive() {
		return maxActive;
	}

	public RedisPoolBuilder setMaxActive(int maxActive) {
		this.maxActive = maxActive;
		return this;
	}

	public int getMaxIdle() {
		return maxIdle;
	}

	public RedisPoolBuilder setMaxIdle(int maxIdle) {
		this.maxIdle = maxIdle;
		return this;
	}
	
	public int getMaxWait() {
		return maxWait;
	}

	public RedisPoolBuilder setMaxWait(int maxWait) {
		this.maxWait = maxWait;
		return this;
	}
	
	public boolean isTestOnBorrow() {
		return testOnBorrow;
	}

	public RedisPoolBuilder setTestOnBorrow(boolean testOnBorrow) {
		this.testOnBorrow = testOnBorrow;
		return this;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public RedisPoolBuilder setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
		return this;
	}

	public int getPort() {
		return port;
	}

	public RedisPoolBuilder setPort(int port) {
		this.port = port;
		return this;
	}

	public RedisPoolBuilder setAuth(String auth) {
		this.auth = auth;
		return this;
	}
	
	public RedisPoolBuilder setEncrypted(boolean isEncrypted) {
		this.isEncrypted = isEncrypted;
		return this;
	}

	public JedisPool jedisPool() {
		JedisPoolConfig config = new JedisPoolConfig();
		config.setMaxTotal(getMaxActive());
		config.setMaxIdle(getMaxIdle());
		config.setMaxWaitMillis(getMaxWait());
		config.setTestOnBorrow(isTestOnBorrow());
		
		String[] ipAndPort = getIpAddress().split(":");
		String ip = "";
		int port = 0;
		if (ipAndPort.length == 1) {
			ip = ipAndPort[0];
			port = getPort();
		} else {
			ip = ipAndPort[0];
			port = Integer.valueOf(ipAndPort[1]);
		}
		logger.info("Connected redis info ip : [" + ip + "] port : [" + port + "]");
		
		if(null == this.auth || this.auth.trim().length() == 0) {
			//no password
			return new JedisPool(config, ip, port);
		}
		
		if(this.isEncrypted) {
			//need encrypt
			this.auth = SimpleCrypt.encryptToDES(SimpleCrypt.getDESKey(), this.auth);
		}
		
		return new JedisPool(config, ip, port, Protocol.DEFAULT_TIMEOUT, this.auth);
	}
}
