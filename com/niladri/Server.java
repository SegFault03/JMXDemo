package com.niladri;

import com.niladri.beans.ProxyMetrics;

import javax.management.*;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.rmi.registry.LocateRegistry;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * JMX Server that exposes MBeans over an RMI connector for remote monitoring.
 *
 * <p>
 * This server performs the following on startup:
 * </p>
 * <ol>
 * <li>Creates an RMI registry on port 3000.</li>
 * <li>Configures authentication and authorization via properties files.</li>
 * <li>Starts a {@link javax.management.remote.JMXConnectorServer} to accept
 * remote connections.</li>
 * <li>Registers a {@link com.niladri.beans.ProxyMetrics} MBean and spawns a
 * background thread
 * that randomly increments its counters to simulate real-world metric
 * updates.</li>
 * </ol>
 *
 * <p>
 * Usage: {@code java -cp . com.niladri.Server} (run from the project root
 * directory).
 * </p>
 *
 * @see com.niladri.beans.ProxyMetrics
 * @see com.niladri.beans.ProxyMetricsMBean
 */
public class Server {
    private static final String CRED_PROPERTIES_PATH = "jmx-credentials.properties";
    private static final String ACCESS_PROPERTIES_PATH = "jmx-access.properties";
    private static MBeanServer mbs;

    /**
     * Private constructor. Initializes the MBeanServer reference to {@code null}.
     * The server is intended to be instantiated only from {@link #main(String[])}.
     */
    private Server() {
        mbs = null;
    }

    /**
     * Starts the JMX server.
     *
     * <p>
     * Creates an RMI registry, retrieves the platform MBeanServer, configures
     * authentication/authorization from properties files, and starts the JMX
     * connector
     * server listening on port 3000.
     * </p>
     *
     * @throws IOException if the RMI registry, connector server, or properties
     *                     files
     *                     cannot be initialized
     */
    private void start() throws IOException {
        // Start an RMI registry on port 3000.
        System.out.println("Creating RMI registry on port 3000...");
        LocateRegistry.createRegistry(3000);

        // Retrieve the PlatformMBeanServer.
        System.out.println("Getting the platform's MBean server...");
        mbs = ManagementFactory.getPlatformMBeanServer();

        // Environment map.
        System.out.println("Initialized the environment map");
        HashMap<String, Object> env = new HashMap<String, Object>();

        // Provide the password file used by the connector server to
        // perform user authentication. The password file is a properties
        // based text file specifying username/password pairs.
        //

        env.put("jmx.remote.x.password.file", CRED_PROPERTIES_PATH);

        // Provide the access level file used by the connector server to
        // perform user authorization. The access level file is a properties
        // based text file specifying username/access level pairs where
        // access level is either "readonly" or "readwrite" access to the
        // MBeanServer operations.
        //
        env.put("jmx.remote.x.access.file", ACCESS_PROPERTIES_PATH);

        // Create an RMI connector server.
        System.out.println("Create an RMI connector server");
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://:3000/jmxrmi");
        JMXConnectorServer cs = JMXConnectorServerFactory.newJMXConnectorServer(url, env, mbs);

        // Start the RMI connector server.
        System.out.println("Start the RMI connector server");
        cs.start();
        System.out.println("Listening on port 3000...");
    }

    /**
     * Application entry point.
     *
     * <p>
     * Instantiates the server, starts it, registers a
     * {@link com.niladri.beans.ProxyMetrics}
     * MBean under the ObjectName
     * {@code com.tcs.icamera.proxy.metrics:type=metrics,proxy_id=1345}, and starts
     * a background
     * thread that randomly increments one of the MBean's counters every 2 seconds.
     * </p>
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        Server server = new Server();
        try {
            // start the server
            server.start();

            // On a successful startup, register mbeans to our mbean server
            ProxyMetrics proxyMetrics = new ProxyMetrics();
            ObjectName objectName = new ObjectName("com.tcs.icamera.proxy.metrics:type=metrics,proxy_id=1345");
            System.out.println("Registering Mbean on the Mbeanserver...");
            mbs.registerMBean(proxyMetrics, objectName);

            // Start a control thread to increment counter and mimic a real-world scenario
            System.out.println("Starting thread to update counters...");
            Thread t = new Thread(() -> {
                while (true) {
                    // Choose counter 1 or 2 randomly
                    int randomNum = ThreadLocalRandom.current().nextInt(1, 3);
                    if (randomNum == 1) {
                        proxyMetrics.incrementCounter1();
                    } else {
                        proxyMetrics.incrementCounter2();
                    }
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            t.start();

            // Wait indefinitely
            while (true) {
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MalformedObjectNameException | NotCompliantMBeanException | InstanceAlreadyExistsException
                | MBeanRegistrationException e) {
            throw new RuntimeException(e);
        }
    }
}
