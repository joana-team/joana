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

package conc.kn;

import java.util.BitSet;
import java.util.Stack;

public class Knapsack5 {
    private static class Item {
        int profit,weight,pos;
        float profitPerWeight;
    }

    int DFSLEVELS = 20;
    int LEVELS = 5;
    BitSet selected;
    int capacity;
    volatile float bestProfit = 0;
    Item[] item;
    PriorityRunQueue prq = new PriorityRunQueue();
    Future done = new Future();
    TerminationGroup tg = new SharedTerminationGroup(done) ;

    public BitSet getSelected()
    throws InterruptedException {
        done.getValue();
        prq.setMaxThreadsWaiting(0);
        searchFactory.terminate();
        BitSet s = new BitSet(item.length);

        for(int i = 0; i < item.length; i++) {
            if (selected.get(i)) s.set(item[i].pos);
        }

        return s;
    }

    public int getProfit()
    throws InterruptedException {
        done.getValue();
        prq.setMaxThreadsWaiting(0);
        searchFactory.terminate();
        return (int)bestProfit;
    }

    class SearchFactory {
        Stack<Search> prev=new Stack<Search>();

        Search make(int i, int rw, int p, BitSet b, TerminationGroup tg) {
            Search g = null;

            synchronized (this) {
                if (!prev.isEmpty()) g = prev.pop();
            }

            if (g == null)
                return new Search(i,rw,p,b,tg);

            else {
                g.i=i; g.rw=rw; g.p=p; g.b=b; g.tg=tg;
                return g;
            }
        }

        synchronized void recycle(Search g) {
            if (prev != null) prev.push(g);
        }

        synchronized void terminate() {
            prev = null;
        }
    }

    SearchFactory searchFactory= new SearchFactory();

    class Search implements Runnable {
        int i; int rw; int p; BitSet b; TerminationGroup tg;

        Search(int i, int rw, int p, BitSet b, TerminationGroup tg){
            this.i=i; this.rw=rw; this.p=p; this.b=b; this.tg=tg;
        }

        public void run(){
            for(;;){
                if (p+rw*item[i].profitPerWeight<bestProfit) break;//return;
                if (i>=LEVELS) {
                    dfs(i,rw,p);
                    break;
                }
                if (rw - item[i].weight >= 0) {
                    // first, start zero's subtree
                    b.clear(i);
                    prq.run(searchFactory.make(i+1,rw,p,
                            (BitSet)b.clone(),tg.fork()),
                            p+rw*item[i+1].profitPerWeight);
                    // then recursively search the one's subtree
                    b.set(i);
                    //gen(i+1,rw-item[i].weight,p+item[i].profit,b,tg);
                    rw-=item[i].weight;
                    p+=item[i].profit;
                    ++i;
                } else { //just recursively search zero subtree
                    b.clear(i);
                    ++i; //gen(i+1,rw,p,b,tg);
                    prq.run(this,p+rw*item[i].profitPerWeight);
                    return;
                }
            }
            tg.terminate();
            searchFactory.recycle(this);
        }

        void dfs(int i, int rw, int p) {
            if (i>=item.length) {
                if (p>bestProfit) {
                    synchronized(Knapsack5.this) {
                        if (p>bestProfit) {
                            bestProfit=p;
                            Knapsack5.this.selected=(BitSet)b.clone();
                            //System.out.println("new best: "+p);
                        }
                    }
                }
                return;
            }
            if (p+rw*item[i].profitPerWeight<bestProfit) return;
            if (rw-item[i].weight>=0) {
                b.set(i);
                dfs(i+1,rw-item[i].weight,p+item[i].profit);
            }
            b.clear(i);
            dfs(i+1,rw,p);
            return;
        }
    }

    public Knapsack5(int[] weights, int[] profits, int capacity, int DFSLEVELS){
        this.DFSLEVELS=DFSLEVELS;

        if (weights.length!=profits.length)
            throw new IllegalArgumentException(
            "0/1 Knapsack: differing numbers of weights and profits");

        if (capacity<=0)
            throw new IllegalArgumentException(
            "0/1 Knapsack: capacity<=0");

        item = new Item[weights.length];
        int i;

        for (i=0; i<weights.length; i++) {
            item[i]=new Item();
            item[i].profit=profits[i];
            item[i].weight=weights[i];
            item[i].pos=i;
            item[i].profitPerWeight=((float)profits[i])/weights[i];
        }

        int j;
        for (j=1; j<item.length; j++) {
            for (i=j; i>0 && item[i].profitPerWeight > item[i-1].profitPerWeight; i--) {
                Item tmp=item[i];
                item[i]=item[i-1];
                item[i-1]=tmp;
            }
        }

        LEVELS=Math.max(1,item.length-DFSLEVELS);
        //****************************
        prq.setWaitTime(10000);
        prq.setMaxThreadsCreated(4);
        //****************************
        prq.run(searchFactory.make(0,capacity,0,new BitSet(item.length),tg), 0);
    }

    public static void main(String[] args) {
        try{
            int num=20;
            int max=100;
            int dfslevels=10;
            int[] p=new int[num];
            int[] w=new int[num];
            int capacity=(int)(num*(max/2.0)*0.5);
            int i;

            for (i=p.length-1;i>=0;i--) {
                p[i]=1+(int)(Math.random()*(max-1));
                w[i]=1+(int)(Math.random()*(max-1));
            }

            System.out.print("p:");
            for (i=0;i<p.length;i++) {
                System.out.print(" "+p[i]);
            }
            System.out.println();

            System.out.print("w:");
            for (i=0;i<p.length;i++) {
                System.out.print(" "+w[i]);
            }
            System.out.println();

            Knapsack5 ks = new Knapsack5(w,p,capacity,dfslevels);
            BitSet s = ks.getSelected();

            System.out.print("s:");
            for (i=0;i<p.length;i++) {
                System.out.print(" "+(s.get(i)?"1":"0")+" ");
            }
            System.out.println();

            System.out.println("Profit: "+ks.getProfit());

        } catch (Exception exc) {
            exc.printStackTrace();
        }

        try{
            int num=50;
            int max=100;
            int dfslevels=10;
            int[] p=new int[num];
            int[] w=new int[num];
            int capacity=(int)(num*(max/2.0)*0.25);
            int i;

            for (i = p.length-1; i >= 0; i--) {
                p[i]=1+(int)(Math.random()*(max-1));
                w[i]=1+(int)(Math.random()*(max-1));
            }

            long starttime=System.currentTimeMillis();
            Knapsack5 ks = new Knapsack5(w,p,capacity,dfslevels);
            ks.getProfit();
            System.out.println("levels: "+num+"  time: "+ (System.currentTimeMillis()-starttime));

        } catch (Exception exc) {
            System.out.println(exc);
        }

        try{
            if (args.length < 2) {
                System.out.println("usage: java Knapsack5 length dfslevels");
                System.exit(0);
            }

            int num=Integer.parseInt(args[0]);
            int max=100;
            int DFSLVLS=Integer.parseInt(args[1]);
            int[] p=new int[num];
            int[] w=new int[num];
            int capacity=(int)(num*(max/2.0)*0.25);
            int i;

            for (i = p.length-1; i >= 0; i--) {
                p[i]=1+(int)(Math.random()*(max-1));
                w[i]=1+(int)(Math.random()*(max-1));
            }

            long starttime=System.currentTimeMillis();
            Knapsack5 ks = new Knapsack5(w,p,capacity,DFSLVLS);
            ks.getProfit();
            System.out.println("Knapsack5\t"+num+"\t"+DFSLVLS+"\t"+ (System.currentTimeMillis()-starttime));

        } catch (Exception exc) {
            System.out.println(exc);
        }
    }
}
