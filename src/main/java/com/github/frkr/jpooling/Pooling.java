/*
 * Copyright (c) 2021, Davi Saranszky Mesquita <davimesquita@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 *  Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 *  Neither the name of this project nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 */

package com.github.frkr.jpooling;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class Pooling {

    //region Properties
    protected final LinkedBlockingDeque<Connection> pool = new LinkedBlockingDeque<Connection>();
    protected final LinkedBlockingDeque<ConnectionIH> alive = new LinkedBlockingDeque<ConnectionIH>();

    private Long pulseMilis;
    private Integer isValidTimeoutSeconds;
    private Integer connections;
    private Boolean forceNewOnStuck;
    private String threadString;
    private Boolean running;
    private String connectionUrl;
    private String user;
    private String pass;
    private Thread service = null;

    //region GET/SETTER
    public synchronized Long getPulseMilis() {
        return pulseMilis;
    }

    public synchronized void setPulseMilis(Long pulseMilis) {
        this.pulseMilis = pulseMilis;
    }

    public synchronized String getConnectionUrl() {
        return connectionUrl;
    }

    public synchronized void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public synchronized String getUser() {
        return user;
    }

    public synchronized void setUser(String user) {
        this.user = user;
    }

    public synchronized String getPass() {
        return pass;
    }

    public synchronized void setPass(String pass) {
        this.pass = pass;
    }

    public synchronized Boolean isRunning() {
        return running;
    }

    public synchronized void setRunning(boolean running) {
        this.running = running;
    }

    public synchronized Integer getConnections() {
        return connections;
    }

    public synchronized void setConnections(Integer connections) {
        this.connections = connections;
    }

    public synchronized Boolean getForceNewOnStuck() {
        return forceNewOnStuck;
    }

    public synchronized void setForceNewOnStuck(Boolean forceNewOnStuck) {
        this.forceNewOnStuck = forceNewOnStuck;
    }

    public synchronized String getThreadString() {
        return threadString;
    }

    public synchronized void setThreadString(String threadString) {
        this.threadString = threadString;
    }

    public synchronized Thread getService() {
        return service;
    }

    public synchronized void setService(Thread service) {
        this.service = service;
    }

    public synchronized Integer getIsValidTimeoutSeconds() {
        return this.isValidTimeoutSeconds;
    }

    public synchronized void setIsValidTimeoutSeconds(Integer isValidTimeoutSeconds) {
        this.isValidTimeoutSeconds = isValidTimeoutSeconds;
    }
    //endregion
    //endregion

    //region Construtores

    /**
     * @see #Pooling(String, String, String)
     */
    public Pooling(String connectionUrl) {
        this(connectionUrl, null, null);
    }

    /**
     * Valores padrões<br/>
     * pulseMilis=300000L; <i>5 minutos</i><br/>
     * isValidTimeoutSeconds=15;<br/>
     * connections=15;<br/>
     * forceNewOnStuck=true;<br/>
     * threadString=jpooling;<br/>
     *
     * @param connectionUrl
     * @param user
     * @param pass
     * @see java.util.concurrent.TimeUnit
     */
    public Pooling(String connectionUrl, String user, String pass) {
        this.pulseMilis = TimeUnit.MINUTES.toMillis(5);
        this.isValidTimeoutSeconds = 15;
        this.connections = 15;
        this.forceNewOnStuck = true;
        this.running = true;
        this.threadString = "jpooling";
        this.connectionUrl = connectionUrl;
        this.user = user;
        this.pass = pass;
        iniciarThread();
    }

    private synchronized void iniciarThread() {
        if (getService() == null || !getService().isAlive()) {
            setService(ThreadPooling.start(this));
        }
    }
    //endregion

    //region Commons

    /**
     * in jtds does not work
     *
     * @param conn Connection
     * @return boolean
     * @throws SQLException isValid may throw errors
     */
    protected boolean isValid(Connection conn) throws SQLException {
        return conn.isValid(this.isValidTimeoutSeconds);
    }

    protected Connection DriverManagergetConnection() throws SQLException {
        if (user == null || pass == null) {
            return DriverManager.getConnection(connectionUrl);
        } else {
            return DriverManager.getConnection(connectionUrl, user, pass);
        }
    }

    /**
     * @return pool size
     */
    public int getStandby() {
        return pool.size();
    }

    /**
     * @return active size
     */
    public int getActive() {
        return alive.size();
    }
    //endregion

    /**
     * @see #open(boolean)
     */
    protected boolean open() {
        return open(false);
    }

    /**
     * @return isRunning() && (pool.size() + alive.size()) < getConnections()
     */
    protected boolean open(boolean force) {
        if (
                force
                        || (isRunning() && (pool.size() + alive.size()) < getConnections())
        ) {
            Connection conn = null;
            try {
                conn = DriverManagergetConnection();
                Thread.yield();
                if (isValid(conn)) {
                    pool.add(conn);
                } else {
                    throw new SQLException("isValid");
                }
            } catch (Throwable e) {
                new Exception(getThreadString() + "-OpenConnection", e).printStackTrace();
                try {
                    conn.close();
                } catch (Exception e2) {
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * @return {@link Connection}
     */
    public Connection getConnection() {
        Exception leak = new Exception(getThreadString() + "-LEAK");
        Exception stuck = new Exception(getThreadString() + "-STUCK");
        long tempoEspera = System.currentTimeMillis() + getPulseMilis();
        while (true) {
            synchronized (this) {
                if (!isRunning()) {
                    setRunning(true);
                    iniciarThread();
                }
            }
            try {
                Connection cn = pool.poll(isValidTimeoutSeconds, TimeUnit.SECONDS);
                if (cn != null) {
                    if (isValid(cn)) {
                        ConnectionIH handler = new ConnectionIH(cn, this, leak);
                        Connection connProxy = (Connection) Proxy.newProxyInstance(
                                ConnectionIH.class.getClassLoader()
                                , new Class<?>[]{Connection.class}
                                , handler
                        );
                        alive.add(handler);
                        return connProxy;
                    }
                }
            } catch (Throwable e) {
                new Exception(getThreadString() + "-PollConnection", e).printStackTrace();
            }
            if (System.currentTimeMillis() > tempoEspera) {
                stuck.printStackTrace();
                CreateConnectionThread.start(this, getForceNewOnStuck());
            } else {
                CreateConnectionThread.start(this);
            }
        }
    }

    protected void close(ConnectionIH connIH) {
        alive.remove(connIH);
        if (!isRunning() || (pool.size() + alive.size() + 1) > getConnections()) {
            try {
                connIH.connection.close();
            } catch (Throwable e) {
            }
        } else {
            pool.add(connIH.connection);
        }
    }

    public void closePool() {
        setRunning(false);
        List<Connection> poolFechar = new LinkedList<Connection>();
        pool.drainTo(poolFechar);
        for (Connection obj : poolFechar) {
            if (obj != null) {
                try {
                    obj.close();
                } catch (Throwable e) {
                }
            }
        }

        List<ConnectionIH> ativasFechar = new LinkedList<ConnectionIH>();
        alive.drainTo(ativasFechar);
        for (ConnectionIH obj : ativasFechar) {
            if (obj != null) {
                try {
                    obj.connection.close();
                } catch (Throwable e) {
                    obj.leak.printStackTrace();
                    new Exception(getThreadString() + "-ClosePool", e).printStackTrace();
                }
            }
        }
    }
}
