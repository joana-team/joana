/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
/*
To accompany High-Performance Java Platform(tm) Computing:
Threads and Networking, published by Prentice Hall PTR and
Sun Microsystems Press.

Threads and Networking Library
Copyright (C) 1999-2000
Thomas W. Christopher and George K. Thiruvathukal

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Library General Public
License as published by the Free Software Foundation; either
version 2 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Library General Public License for more details.

You should have received a copy of the GNU Library General Public
License along with this library; if not, write to the
Free Software Foundation, Inc., 59 Temple Place - Suite 330,
Boston, MA  02111-1307, USA.
 */

package conc.sq;

import java.util.Hashtable;

class SharedTableOfQueues<Key, Value> extends Monitor {

    Hashtable<Key, Folder> tbl = new Hashtable<Key, Folder>();

    private class Folder {
        volatile QueueComponent<Value> q = new QueueComponent<Value>();
        volatile Condition notEmpty = new Condition();
        volatile int numWaiting=0;
    }

    public void put(Key key, Value value) {
        enter();
        Folder f = tbl.get(key);
        if (f == null) tbl.put(key, f = new Folder());
        f.q.put(value);
        f.notEmpty.leaveWithSignal();
    }

    public Value get(Key key) throws InterruptedException {
        Folder f = null;
        enter();
        try {
            f = tbl.get(key);
            if (f == null) tbl.put(key, f = new Folder());
            f.numWaiting++;
            if (f.q.isEmpty()) f.notEmpty.await();
            f.numWaiting--;
            return f.q.get();

        } finally {
            if (f != null
                    && f.q.isEmpty()
                    && f.numWaiting==0) {
                tbl.remove(key);
            }
            leave();
        }
    }

    public Value getSkip(Key key) {
        Folder f = null;
        enter();
        try {
            f = tbl.get(key);
            if (f == null || f.q.isEmpty()) {
                return null;
            }
            return f.q.get();

        } finally {
            if (f != null
                    && f.q.isEmpty()
                    && f.numWaiting==0) {
                tbl.remove(key);
            }
            leave();
        }
    }

}
