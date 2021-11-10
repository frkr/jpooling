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

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PoolingFreshing extends Pooling {

    //region GET/SETTER
    private Integer refresh;

    public synchronized Integer getRefresh() {
        return refresh;
    }

    public synchronized void setRefresh(Integer refresh) {
        this.refresh = refresh;
    }
    //endregion

    //region Construtor
    public PoolingFreshing(
            String connectionUrl,
            int refresh
    ) {
        super(connectionUrl);
        this.refresh = refresh;
    }

    public PoolingFreshing(
            String connectionUrl,
            String user,
            String pass,
            int refresh
    ) {
        super(connectionUrl, user, pass);
        this.refresh = refresh;
    }
    //endregion

    protected Map<Connection, AtomicInteger> usadas = new HashMap<Connection, AtomicInteger>();

    @Override
    protected void close(ConnectionIH connIH) {
        AtomicInteger used;
        synchronized (usadas) {
            used = usadas.get(connIH.connection);
            if (used == null) {
                used = new AtomicInteger(0);
                usadas.put(connIH.connection, used);
            }
        }
        if (used.incrementAndGet() < getRefresh()) {
            super.close(connIH);
        } else {
            alive.remove(connIH);
            usadas.remove(connIH.connection);
            try {
                connIH.connection.close();
            } catch (Throwable e) {
            }
            CreateConnectionThread.start(this);
        }
    }

}
