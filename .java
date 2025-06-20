package OrderManagement;

import java.net.Authenticator.RequestorType;

public class OrderRequest {
	static long nextId = 1; // static ID counter

    int symbolId;
    double price;
    long qty;
    char side;
    long orderId;
    RequestType type;
 public OrderRequest(int symbolId, double price, long qty, char side, RequestType type) {
        this.symbolId = symbolId;
        this.price = price;
        this.qty = qty;
        this.side = side;
        this.type = type;
        this.orderId = nextId++; // 👈 Auto increment ID
    }
 // Getter if needed
 public long getOrderId() {
     return orderId;
 }
}

import java.net.CacheResponse;
import java.net.ResponseCache;
import java.net.http.HttpResponse.ResponseInfo;
import javax.lang.model.type.ReferenceType;

public class OrderResponse {
	 long orderId;
	    ResponseType responseType;

	    public OrderResponse(long orderId, ResponseType responseType) {
	        this.orderId = orderId;
	        this.responseType = responseType;
	    }
	}

	package OrderManagement;

import java.io.FileWriter;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OrderManagement<sentTimestamps> {
	 private final int maxOrdersPerSecond;
	    private final Queue<OrderRequest> queue = new LinkedList<>();
	    private final Map<Long, OrderRequest> queuedOrders = new HashMap<>();
	    private final Map<Long, Instant> sentTimestamps = new ConcurrentHashMap<>();
	    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	    private final Object lock = new Object();

	    private int startHour = 10, endHour = 13;
	    private int orderCount = 0;
	    private long currentSecond = Instant.now().getEpochSecond();

	    public OrderManagement(int maxRate) {
	        this.maxOrdersPerSecond = maxRate;
	        scheduler.scheduleAtFixedRate(this::dispatchOrders, 1, 1, TimeUnit.SECONDS);
	    }

	    public void onData(OrderRequest request) {
	        synchronized (lock) {
	            if (!withinTradingHours()) {
	                System.out.println("Order outside trading window: " + request.orderId);
	                return;
	            }

	            long nowSec = Instant.now().getEpochSecond();
	            if (nowSec != currentSecond) {
	                currentSecond = nowSec;
	                orderCount = 0;
	            }
	        }

            if (orderCount < maxOrdersPerSecond) {
                send(request);
                orderCount++;
            } else {
                queue.offer(request);
                queuedOrders.put(request.orderId, request);
            }
        }

    public void onData(OrderResponse response) {
        Instant now = Instant.now();
        Instant sentTime = sentTimestamps.remove(response.orderId);
        if (sentTime != null) {
            long latency = Duration.between(sentTime, now).toMillis();
            logResponse(response, latency);
        }
    }

    public void modifyOrCancel(RequestType type, OrderRequest request) {
        synchronized (lock) {
            if (queuedOrders.containsKey(request.orderId)) {
                if (type == RequestType.Modify) {
                    OrderRequest queued = queuedOrders.get(request.orderId);
                    queued.price = request.price;
                    queued.qty = request.qty;
                    System.out.println("Modified order in queue: " + request.orderId);
                } else if (type == RequestType.Modify) {
                	 queue.removeIf(o -> o.orderId == request.orderId);
                     queuedOrders.remove(request.orderId);
                     System.out.println("Canceled order in queue: " + request.orderId);
                 }
             }
         }
     }

     private boolean withinTradingHours() {
         LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));
         return !now.isBefore(LocalTime.of(startHour, 0)) && now.isBefore(LocalTime.of(endHour, 0));
     }

     private void dispatchOrders() {
         synchronized (lock) {
             orderCount = 0;
             currentSecond = Instant.now().getEpochSecond();
             while (!queue.isEmpty() && orderCount < maxOrdersPerSecond) {
                 OrderRequest req = queue.poll();
                 queuedOrders.remove(req.orderId);
                 send(req);
                 orderCount++;
             }
         }
     }

     private void send(OrderRequest request)
     {
    	 if (request.orderId == 0) {
    		    System.out.println("⚠️ Warning: Order sent with ID 0 — check object creation.");
    		}
         sentTimestamps.put(request.orderId, Instant.now());
         System.out.println("Sent to exchange: " + request.orderId);
                }


private void logResponse(OrderResponse response, long latency) {
    try (FileWriter fw = new FileWriter("order_latency_log.txt", true)) {
        fw.write("OrderID: " + response.orderId +
                ", Response: " + response.responseType +
                ", Latency: " + latency + "ms\n");
    } catch (IOException e) {
        e.printStackTrace();
    }
}
}

package OrderManagement;

public class Main {

	   public static void main(String[] args) throws InterruptedException {
	        OrderManagement oms = new OrderManagement(2);

	        OrderRequest o1 = new OrderRequest(1, 100.0, 10, 'B',  RequestType.New);
	        System.out.println("Order ID: " + o1.getOrderId());  // Output: 1
	        OrderRequest o2 = new OrderRequest(1, 101.0, 15, 'S',  RequestType.New);
	        OrderRequest o3 = new OrderRequest(1, 102.0, 5, 'B',  RequestType.New);

	        oms.onData(o1);
	        oms.onData(o2);
	        oms.onData(o3); // will be queued

	        Thread.sleep(2000);

	        OrderRequest modify = new OrderRequest(1, 105.0, 50, 'B',  RequestType.Modify);
	        oms.modifyOrCancel(RequestType.Modify, modify);

	        OrderResponse r1 = new OrderResponse(1L, ResponseType.Accept);
	        oms.onData(r1);
	    }
	


	}
package OrderManagement;
import java.io.FileWriter;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public enum RequestType {
	Unknown, New, Modify, Cancel
	}

	

package OrderManagement;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

public enum ResponseType {
	Unknown, Accept1, Reject, Accept
	}






    