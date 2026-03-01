# JMX Remote Monitoring Demo

A demonstration project showcasing Java Management Extensions (JMX) remote monitoring via RMI. A **Server** exposes MBean attributes over a JMX connector, and a **Client** connects remotely to read those attributes in real time — with authentication and access control.

## Project Structure

```
JmxDemo/
├── com/niladri/
│   ├── Server.java              # JMX server with RMI connector
│   └── Client.java              # JMX client that connects remotely
├── com/niladri/beans/
│   ├── ProxyMetricsMBean.java   # MBean interface definition
│   └── ProxyMetrics.java        # MBean implementation
├── jmx-credentials.properties   # Username/password pairs for authentication
├── jmx-access.properties        # Username/access-level pairs (readonly/readwrite)
└── README.md
```

## How It Works

### Server (`com.niladri.Server`)

1. Creates an RMI registry on **port 3000**.
2. Retrieves the platform `MBeanServer`.
3. Configures JMX authentication (`jmx-credentials.properties`) and authorization (`jmx-access.properties`).
4. Starts a `JMXConnectorServer` exposing the MBean server over RMI.
5. Registers a `ProxyMetrics` MBean with the ObjectName `com.tcs.icamera.proxy.metrics:type=metrics,proxy_id=1345`.
6. Spawns a background thread that randomly increments `Counter1` or `Counter2` every 2 seconds to simulate real-world metric updates.

### Client (`com.niladri.Client`)

1. Reads credentials from `jmx-credentials.properties`.
2. Connects to the server's JMX RMI endpoint at `service:jmx:rmi:///jndi/rmi://:3000/jmxrmi`.
3. Creates an MBean proxy (`JMX.newMBeanProxy`) for type-safe attribute access.
4. Polls and prints all MBean attributes every 2 seconds.

### MBean (`com.niladri.beans.ProxyMetrics`)

Exposes the following attributes via the `ProxyMetricsMBean` interface:

| Attribute        | Type            | Description                        |
|------------------|-----------------|------------------------------------|
| `Counter1`       | `int`           | Incrementing counter               |
| `Counter2`       | `int`           | Incrementing counter               |
| `FlagState`      | `boolean`       | Boolean flag (default: `false`)    |
| `PropertyName`   | `String`        | A string property                  |
| `ArrayOfStrings` | `List<String>`  | A list of strings                  |

## Prerequisites

- **Java JDK 8+** (uses `javax.management` APIs)

## Usage

All commands must be run from the **project root** (`JmxDemo/`), since the `.properties` files are referenced via relative paths.

### 1. Compile

```bash
javac com/niladri/beans/ProxyMetricsMBean.java \
      com/niladri/beans/ProxyMetrics.java \
      com/niladri/Client.java \
      com/niladri/Server.java
```

### 2. Start the Server

```bash
java -cp . com.niladri.Server
```

Expected output:

```
Creating RMI registry on port 3000...
Getting the platform's MBean server...
Initialized the environment map
Create an RMI connector server
Start the RMI connector server
Listening on port 3000...
Registering Mbean on the Mbeanserver...
Starting thread to update counters...
```

### 3. Start the Client (in a separate terminal)

```bash
java -cp . com.niladri.Client
```

Expected output:

```
Connection successful!
Starting thread to print attributes every 2 secs...
Printing attributes using proxy:
Counter1: 2
Counter2: 3
PropertyName: InitialPropertyName
FlagState: false
ArrayOfStrings: [Hello, World]
```

The client will continue polling every 2 seconds, and you will see the counters update in real time.

## Authentication

Authentication is configured via two properties files in the project root:

- **`jmx-credentials.properties`** — Maps usernames to passwords (whitespace-separated).
- **`jmx-access.properties`** — Maps usernames to access levels (`readonly` or `readwrite`).

> **Note:** In a production environment, these files should have restricted filesystem permissions and should not be committed to version control.
