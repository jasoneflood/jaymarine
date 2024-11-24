package com.thejasonengine.messaging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

public class ClusteredEventBus 
{
	private static final Logger LOGGER = LogManager.getLogger(ClusteredEventBus.class);
    public static void main(String[] args) 
    {
        HazelcastClusterManager mgr = new HazelcastClusterManager();
        VertxOptions options = new VertxOptions().setClusterManager(mgr);

        Vertx.clusteredVertx(options, res -> {
            if (res.succeeded()) {
                Vertx vertx = res.result();
                EventBus eventBus = vertx.eventBus();

                eventBus.consumer("cluster-data", message -> {
                   LOGGER.info("Received message: " + message.body());
                });

                eventBus.publish("cluster-data", "Hello from the cluster!");
            } else {
                LOGGER.error("Failed to start clustered Vert.x");
            }
        });
    }
}