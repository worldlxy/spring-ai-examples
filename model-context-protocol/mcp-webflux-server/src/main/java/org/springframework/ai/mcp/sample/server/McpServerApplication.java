package org.springframework.ai.mcp.sample.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class McpServerApplication {

	public static void main(String[] args) {

		// var stackThread = new Thread() {
		// @Override
		// public void run() {
		// try {
		// while (true) {
		// Thread.sleep(5000);
		// java.util.Collection<java.lang.StackTraceElement[]> a1 =
		// java.lang.Thread.getAllStackTraces()
		// .values();
		// for (java.lang.StackTraceElement[] a2 : a1) {
		// System.out.println("==========");
		// for (java.lang.StackTraceElement a3 : a2) {
		// System.out.println(a3.toString());
		// }
		// }
		// }
		// } catch (InterruptedException e) {
		// e.printStackTrace();
		// }
		// }
		// };
		// stackThread.setDaemon(false);
		// stackThread.start();

		SpringApplication.run(McpServerApplication.class, args);
	}

}
