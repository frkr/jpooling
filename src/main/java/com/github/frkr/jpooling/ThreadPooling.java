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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class ThreadPooling implements Runnable {

    private final Pooling pool;

    private ThreadPooling(Pooling pool) {
        this.pool = pool;
    }

    public static Thread start(Pooling pool) {
        Thread th = new Thread(new ThreadPooling(pool));
        th.setName(pool.getThreadString() + "-" + th.getId());
        th.setDaemon(true);
        th.start();
        return th;
    }

    public void run() {
        while (pool.isRunning()) {
            criarConexoes();
            try {
                TimeUnit.MILLISECONDS.sleep(pool.getPulseMilis());
            } catch (Exception e) {
            }
            if (pool.alive.size() == 0) {
                pool.closePool();
            } else {
                long now = System.currentTimeMillis();
                List<ConnectionIH> vivas = new LinkedList<ConnectionIH>(pool.alive);
                for (ConnectionIH obj : vivas) {
                    if ((obj.time.get() + pool.getPulseMilis()) < now) {
                        obj.leak.printStackTrace();
                    }
                }
            }
        }
        pool.closePool();
    }

    private void criarConexoes() {
        while (pool.open()) ;
    }

}
