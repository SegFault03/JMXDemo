package com.niladri;

import com.niladri.beans.ProxyMetricsMBean;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * JMX Client that connects to a remote MBean server over RMI and reads MBean
 * attributes.
 *
 * <p>
 * This client authenticates using credentials read from a properties file,
 * connects to the JMX RMI endpoint, and provides two methods for reading MBean
 * attributes: direct attribute queries and type-safe MBean proxies.
 * </p>
 *
 * <p>
 * Usage: {@code java -cp . com.niladri.Client} (run from the project root
 * directory).
 * </p>
 *
 * @see com.niladri.beans.ProxyMetricsMBean
 * @see javax.management.JMX#newMBeanProxy
 */
public class Client {
    private static final String CRED_PROPERTIES_PATH = "jmx-credentials.properties";
    private static final String rmiUrl = "service:jmx:rmi:///jndi/rmi://:3000/jmxrmi";
    private static MBeanServerConnection mbsc;

    /**
     * Private constructor. Initializes the MBeanServerConnection to {@code null}.
     * The client is intended to be instantiated only from {@link #main(String[])}.
     */
    private Client() {
        mbsc = null;
    }

    /**
     * Parses a JMX credentials file and extracts the username and password.
     *
     * <p>
     * The file is expected to contain lines in the format
     * {@code username password},
     * separated by whitespace. Empty lines and lines starting with {@code #} are
     * ignored.
     * If multiple entries exist, only the last one is used.
     * </p>
     *
     * @param path the path to the credentials properties file
     * @return a two-element {@code String} array where index 0 is the username
     *         and index 1 is the password
     * @throws IOException              if the file cannot be read
     * @throws IllegalArgumentException if a line does not contain exactly two
     *                                  tokens
     */
    public static String[] parse(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path);

        String[] creds = new String[2];

        for (String line : lines) {

            line = line.trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] parts = line.split("\\s+");

            if (parts.length != 2) {
                throw new IllegalArgumentException(
                        "Invalid JMX password file entry: " + line);
            }

            String username = parts[0];
            String password = parts[1];

            creds[0] = username;
            creds[1] = password;
        }

        return creds;
    }

    /**
     * Establishes a connection to the remote JMX server.
     *
     * <p>
     * Reads credentials from the properties file, builds a JMX environment map
     * with the credentials, and connects to the RMI endpoint. On success, the
     * {@link MBeanServerConnection} is stored for subsequent attribute queries.
     * </p>
     *
     * @throws IOException                  if the connection or credential file
     *                                      read fails
     * @throws MalformedObjectNameException if an internal ObjectName is malformed
     * @throws InstanceNotFoundException    if a required MBean instance is not
     *                                      found
     */
    private void connect() throws IOException, MalformedObjectNameException, InstanceNotFoundException {

        // Provide credentials required by server for user authentication (read from
        // .properties)
        HashMap<String, Object> env = new HashMap<>();
        String[] creds = parse(Paths.get(CRED_PROPERTIES_PATH));
        env.put(JMXConnector.CREDENTIALS, creds);

        // Create JMXServiceURL of JMX Connector (must be known in advance)
        JMXServiceURL url = new JMXServiceURL(rmiUrl);

        // Get JMX connector
        JMXConnector jmxc = JMXConnectorFactory.connect(url, env);

        // Get MBean server connection
        mbsc = jmxc.getMBeanServerConnection();

        System.out.println("Connection successful!");
    }

    /**
     * Retrieves and prints MBean attributes using direct
     * {@link MBeanServerConnection} queries.
     *
     * <p>
     * First introspects the MBean to list all readable attribute names and types,
     * then fetches a batch of specific attributes and prints their values.
     * This method is an alternative to the proxy-based approach in
     * {@link #getAttributesViaProxy()}.
     * </p>
     *
     * @throws MalformedObjectNameException if the MBean ObjectName is malformed
     * @throws ReflectionException          if a reflection error occurs on the
     *                                      server side
     * @throws InstanceNotFoundException    if the target MBean is not registered
     * @throws IOException                  if a communication error occurs
     * @throws IntrospectionException       if MBean introspection fails
     */
    public void printAttributes() throws MalformedObjectNameException, ReflectionException, InstanceNotFoundException,
            IOException, IntrospectionException {
        ObjectName objectName = new ObjectName("com.tcs.icamera.proxy.metrics:type=metrics,proxy_id=1345");
        if (mbsc == null) {
            return;
        }

        // Check if the object we're searching for exists
        Set<ObjectName> myMbean = mbsc.queryNames(objectName, null);
        if (myMbean.size() == 1) {
            MBeanInfo mbeanInfo = mbsc.getMBeanInfo(objectName);
            MBeanAttributeInfo[] mbeanAttributeInfos = mbeanInfo.getAttributes();

            // Retrieve all the attribute names and their types
            for (MBeanAttributeInfo attribInfo : mbeanAttributeInfos) {
                if (attribInfo.isReadable()) {
                    String attribName = attribInfo.getName();
                    String attribReturnType = attribInfo.getType();
                    System.out.printf("%s: %s\n", attribName, attribReturnType);
                }
            }
        }

        // Note: attributes will always be in CamelCase
        String[] attributes = { "Counter1", "Counter2", "PropertyName", "FlagState", "ArrayOfStrings" };

        AttributeList attributeList = mbsc.getAttributes(objectName, attributes);
        if (attributeList.size() == attributes.length) {
            System.out.println("All attributes found!");
        }

        // Print each attribute
        for (Attribute a : attributeList.asList()) {
            // a.getValue() returns an object - will be of the same type as the one in the
            // original Mbean
            // ...so both primitives and user-defined classes are allowed (int, boolean,
            // List, etc.)
            System.out.printf("%s: %s\n", a.getName(), a.getValue());
        }
    }

    /**
     * Retrieves and prints MBean attributes using a type-safe MBean proxy.
     *
     * <p>
     * Creates a local proxy via {@link JMX#newMBeanProxy} that implements
     * {@link com.niladri.beans.ProxyMetricsMBean}, allowing attribute access
     * through
     * standard getter methods as if the MBean were a local object.
     * </p>
     *
     * @throws IOException                  if a communication error occurs
     * @throws MalformedObjectNameException if the MBean ObjectName is malformed
     * @throws InstanceNotFoundException    if the target MBean is not registered
     */
    public void getAttributesViaProxy() throws IOException, MalformedObjectNameException, InstanceNotFoundException {
        ObjectName objectName = new ObjectName("com.tcs.icamera.proxy.metrics:type=metrics,proxy_id=1345");
        if (mbsc == null) {
            return;
        }

        // Check if the object we're searching for exists
        Set<ObjectName> myMbean = mbsc.queryNames(objectName, null);
        if (myMbean.isEmpty()) {
            return;
        }

        // Create a 'proxy' (not to be confused with iCamera proxy) which will act as if
        // the Mbean was locally available to us
        ProxyMetricsMBean mbeanProxy = JMX.newMBeanProxy(mbsc, objectName,
                ProxyMetricsMBean.class, false);

        // We can now conveniently cast them into appropriate types and act on them as
        // if they were a local instance stored in our own JVM:
        int counter1 = mbeanProxy.getCounter1();
        int counter2 = mbeanProxy.getCounter2();
        boolean flagState = mbeanProxy.getFlagState();
        String propertyName = mbeanProxy.getPropertyName();
        List<String> arrayOfStrings = mbeanProxy.getArrayOfStrings();

        System.out.println("Printing attributes using proxy:");
        System.out.println("Counter1: " + counter1);
        System.out.println("Counter2: " + counter2);
        System.out.println("PropertyName: " + propertyName);
        System.out.println("FlagState: " + flagState);
        System.out.println("ArrayOfStrings: " + arrayOfStrings);
    }

    /**
     * Application entry point.
     *
     * <p>
     * Creates a client, connects to the remote JMX server, and starts a background
     * thread that calls {@link #getAttributesViaProxy()} every 2 seconds to
     * continuously
     * poll and print MBean attribute values.
     * </p>
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            Client client = new Client();
            client.connect();
            // client.printAttributes();
            System.out.println("Starting thread to print attributes every 2 secs...");
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        client.getAttributesViaProxy();
                        Thread.sleep(2000);
                    }
                } catch (IOException | MalformedObjectNameException | InstanceNotFoundException
                        | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
            t.start();
        } catch (IOException | MalformedObjectNameException | InstanceNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
