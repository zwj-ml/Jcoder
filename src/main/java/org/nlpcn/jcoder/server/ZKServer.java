package org.nlpcn.jcoder.server;

import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import org.nlpcn.jcoder.util.StaticValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public class ZKServer extends Thread {
	private static final Logger LOG = LoggerFactory.getLogger(H2Server.class);

	private static boolean start = false;

	private ZKServer() {
	}

	public synchronized static void startServer() {
		if (start) {
			return;
		}

		new ZKServer().start();
	}

	public static void stopServer() {
		//:TODO not support
	}

	@Override
	public void run() {
		Properties props = new Properties();
		props.setProperty("tickTime", "2000");
		props.setProperty("dataDir", new File(System.getProperty("java.io.tmpdir"), "zookeeper").getAbsolutePath());
		props.setProperty("clientPort", String.valueOf(StaticValue.PORT + 2));
		props.setProperty("initLimit", "10");
		props.setProperty("syncLimit", "5");

		QuorumPeerConfig quorumConfig = new QuorumPeerConfig();
		try {
			LOG.info("start zk server on port : " + (StaticValue.PORT + 2));
			quorumConfig.parseProperties(props);
			final ZooKeeperServerMain zkServer = new ZooKeeperServerMain();
			final ServerConfig config = new ServerConfig();
			config.readFrom(quorumConfig);
			zkServer.runFromConfig(config);
		} catch (Exception e) {
			LOG.error("Start standalone server faile", e);
			System.exit(-1);
		}
	}
}
