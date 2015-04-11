package org.ansj.elasticsearch.index.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

import org.ansj.elasticsearch.pubsub.redis.AddTermRedisPubSub;
import org.ansj.elasticsearch.pubsub.redis.RedisPoolBuilder;
import org.ansj.elasticsearch.pubsub.redis.RedisUtils;
import org.ansj.splitWord.analysis.ToAnalysis;
import org.ansj.util.MyStaticValue;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.nlpcn.commons.lang.util.IOUtil;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class AnsjElasticConfigurator {
	public static ESLogger logger = Loggers.getLogger("ansj-analyzer");
	private static boolean loaded = false;
	public static Set<String> filter;
	public static boolean pstemming = false;
	public static Environment environment;
	public static String DEFAULT_USER_LIB_PATH = "ansj/user";
	public static String DEFAULT_AMB_FILE_LIB_PATH = "ansj/ambiguity.dic";
	public static String DEFAULT_STOP_FILE_LIB_PATH = "ansj/stopLibrary.dic";
	public static boolean DEFAULT_IS_NAME_RECOGNITION = false;
	public static boolean DEFAULT_IS_NUM_RECOGNITION = true;
	public static boolean DEFAUT_IS_QUANTIFIE_RRECOGNITION = true;

	public static void init(Settings indexSettings, Settings settings) {
		//如果已经加载过了直接返回
		if (isLoaded()) {
			return;
		}
		
		logger.debug("indexSettings are as below---------------------");
		DebugSetting(indexSettings);
		logger.debug("settings are as below---------------------");
		DebugSetting(settings);
		
		environment = new Environment(indexSettings);
		initConfigPath(settings);
		boolean enabledStopFilter = settings.getAsBoolean("enabled_stop_filter", false);
		if (enabledStopFilter) {
			loadFilter(settings);
		}
		
		try {
			preheat();
			logger.info("Ansj tokenizer had been preheated success!");
		} catch (Exception e) {
			logger.error("Ansj tokenizer preheat failed, please check the path!");
		}
		initRedis(settings);
		setLoaded(true);
	}

	private static void initRedis(final Settings settings) {
		if (null == settings.get("redis.ip")) {
			logger.info("Unable to find any configurations of redis, skip redis feature!");
			return;
		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				RedisPoolBuilder redisPoolBuilder = new RedisPoolBuilder();
				//最大活动连接数
				int maxActive = settings.getAsInt("redis.pool.maxactive",
						redisPoolBuilder.getMaxActive());
				
				int maxIdle = settings.getAsInt("redis.pool.maxidle",
						redisPoolBuilder.getMaxIdle());
				
				//表示当borrow一个jedis实例时，最大的等待时间，如果超过等待时间，
				//则直接抛出JedisConnectionException
				int maxWait = settings.getAsInt("redis.pool.maxwait",
						redisPoolBuilder.getMaxWait());
				
				//在borrow一个jedis实例时，是否提前进行alidate操作；如果为true，则得到的jedis实例均是可用的
				boolean testOnBorrow = settings.getAsBoolean(
						"redis.pool.testonborrow",
						redisPoolBuilder.isTestOnBorrow());
				
				// add auth
				boolean isEncrypt = settings.getAsBoolean("redis.encrypt", false);
				String auth = settings.get("redis.auth");
				
				logger.debug("maxActive: {}, maxIdle: {} ,maxWait: {}, testOnBorrow: {}", maxActive, maxIdle, maxWait, testOnBorrow);

				String ipAndport = settings.get("redis.ip", redisPoolBuilder.getIpAddress());
				int port = settings.getAsInt("redis.port", redisPoolBuilder.getPort());
				String channel = settings.get("redis.channel", "ansj_term");
				
				logger.debug("ip:{},port:{},channel:{}", ipAndport, port, channel);

				JedisPool pool = redisPoolBuilder.setMaxActive(maxActive)
						.setMaxIdle(maxIdle).setMaxWait(maxWait)
						.setTestOnBorrow(testOnBorrow).setIpAddress(ipAndport)
						.setPort(port).setAuth(auth).setEncrypted(isEncrypt).jedisPool();
				RedisUtils.setJedisPool(pool);
				final Jedis jedis = RedisUtils.getConnection();

				logger.debug("pool: {},jedis:", (pool == null), (jedis == null));
				logger.info("redis daemon threads are prepared, ip:{}, port:{}, channel:{}", ipAndport, port, channel);
				jedis.subscribe(new AddTermRedisPubSub(), new String[] { channel });
				RedisUtils.closeConnection(jedis);
			}
		}).start();

	}

	private static void preheat() {
		ToAnalysis.parse("一个词");
	}

	private static void initConfigPath(Settings settings) {
		// 是否提取词干
		pstemming = settings.getAsBoolean("pstemming", false);
		// 用户自定义辞典
		File path = new File(environment.configFile(), settings.get("user_path", DEFAULT_USER_LIB_PATH));
		
		MyStaticValue.userLibrary = path.getAbsolutePath();
		logger.debug("User dictionary path:{}", MyStaticValue.userLibrary);
		// 用户自定义辞典
		path = new File(environment.configFile(), settings.get("ambiguity", DEFAULT_AMB_FILE_LIB_PATH));
		
		MyStaticValue.ambiguityLibrary = path.getAbsolutePath();
		logger.debug("Ambiguity dictionary path:{}", MyStaticValue.ambiguityLibrary);

		MyStaticValue.isNameRecognition = settings.getAsBoolean("is_name", DEFAULT_IS_NAME_RECOGNITION);

		MyStaticValue.isNumRecognition = settings.getAsBoolean("is_num", DEFAULT_IS_NUM_RECOGNITION);

		MyStaticValue.isQuantifierRecognition = settings.getAsBoolean("is_quantifier", DEFAUT_IS_QUANTIFIE_RRECOGNITION);
	}

	private static void loadFilter(Settings settings) {
		Set<String> filters = new HashSet<String>();
		String stopLibraryPath = settings.get("stop_path", DEFAULT_STOP_FILE_LIB_PATH);

		if (stopLibraryPath == null) {
			return;
		}

		File stopLibrary = new File(environment.configFile(), stopLibraryPath);
		logger.debug("Stop word dictionary path:{}", stopLibrary.getAbsolutePath());
		if (!stopLibrary.isFile()) {
			logger.info("Can't find the file: {}, no such file or directory exists!", stopLibraryPath);
			emptyFilter();
			setLoaded(true);
			return;
		}

		BufferedReader br;
		try {
			br = IOUtil.getReader(stopLibrary.getAbsolutePath(), "UTF-8");
			String temp = null;
			while ((temp = br.readLine()) != null) {
				filters.add(temp);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		filter = filters;
		logger.info("ansj stop word dictionary are loaded!");
	}

	private static void emptyFilter() {
		filter = new HashSet<String>();
	}

	public static boolean isLoaded() {
		return loaded;
	}

	public static void setLoaded(boolean loaded) {
		AnsjElasticConfigurator.loaded = loaded;
	}
	
	public static void DebugSetting(Settings settings) {
		if(logger.isDebugEnabled() && null != settings) {
			ImmutableMap<String, String> settingMap = settings.getAsMap();
			logger.debug(settingMap.toString());
		}
	}

}
