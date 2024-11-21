package com.thejasonengine.dashboard;

import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;

/*
  jo_worker.put("workerName", "controller");
  jo_worker.put("version", "hello");
  jo_worker.put("data", "23");
  jo_worker.put("worker_ip", "1");
  jo_worker.put("epoch", "11719750005");
*/


public final class Dashboard 
{
	private static JsonArray workerData =  new JsonArray();;

	/**********************************************/
	public static JsonArray getWorkerData() 
	{
		return workerData;
	}
	public static void setWorkerData(JsonArray workerDataUpdated) 
	{
		Dashboard.workerData = workerDataUpdated;
	}
	/**********************************************/
}
